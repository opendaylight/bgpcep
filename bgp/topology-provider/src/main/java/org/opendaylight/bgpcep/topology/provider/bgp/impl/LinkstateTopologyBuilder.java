/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology.provider.bgp.impl;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.bgpcep.topology.provider.bgp.AbstractLocRIBListener;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.link.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.node.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.prefix.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.isis.node.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.node.identifier.c.router.identifier.c.isis.pseudonode.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoPseudonodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.IsoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.OspfLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.OspfNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.prefix.attributes.OspfPrefixAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

final class LinkstateTopologyBuilder extends AbstractLocRIBListener<LinkstateRoute> {
	LinkstateTopologyBuilder(final InstanceIdentifier<Topology> topology) {
		super(topology, LinkstateRoute.class);
	}

	private String buildNamePrefix(final LinkstateRoute route) {
		final StringBuilder sb = new StringBuilder();

		if (route.getDistinguisher() != null) {
			sb.append(route.getDistinguisher().getValue().toString()).append(':');
		}
		sb.append(route.getProtocolId().toString()).append(':').append(route.getIdentifier().getValue().toString()).append(':');
		return sb.toString();
	}

	private LinkId buildLinkId(final String pfx, final Link link) {
		final StringBuilder sb = new StringBuilder(pfx);

		// FIXME: finish this

		return new LinkId(sb.toString());
	}

	private NodeId buildNodeId(final String pfx, final NodeIdentifier node) {
		final StringBuilder sb = new StringBuilder(pfx);

		// FIXME: finish this

		return new NodeId(sb.toString());
	}

	private InstanceIdentifier<?> buildLinkIdentifier(final String pfx, final Link l) {
		return InstanceIdentifier.builder(getTopology()).node(
				org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link.class,
				new LinkKey(buildLinkId(pfx, l))).toInstance();
	}

	private static Float bandwidthToFloat(final Bandwidth bandwidth) {
		return ByteBuffer.wrap(bandwidth.getValue()).getFloat();
	}

	private static BigDecimal bandwidthToBigDecimal(final Bandwidth bandwidth) {
		return BigDecimal.valueOf(bandwidthToFloat(bandwidth));
	}

	private static List<UnreservedBandwidth> unreservedBandwidthList(final List<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.UnreservedBandwidth> input) {
		final List<UnreservedBandwidth> ret = new ArrayList<>(input.size());

		for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.UnreservedBandwidth i : input) {
			ret.add(new UnreservedBandwidthBuilder().
					setBandwidth(bandwidthToBigDecimal(i.getBandwidth())).
					setKey(new UnreservedBandwidthKey(i.getPriority())).build());
		}

		return ret;
	}


	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1 isisLinkAttributes(final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder tb =
				new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder();

		tb.setColor(la.getAdminGroup().getValue());
		tb.setTeDefaultMetric(la.getTeMetric().getValue());
		tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
		tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
		tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder().setIsisLinkAttributes(
				new IsisLinkAttributesBuilder().setMultiTopologyId(topologyIdentifier.getValue().shortValue()).
				setTed(tb.build()).build()).build();
	}


	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1 ospfLinkAttributes(final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder tb =
				new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder();

		tb.setColor(la.getAdminGroup().getValue());
		tb.setTeDefaultMetric(la.getTeMetric().getValue());
		tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
		tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
		tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1Builder().setOspfLinkAttributes(
				new OspfLinkAttributesBuilder().
				setMultiTopologyId(topologyIdentifier.getValue().shortValue()).
				setTed(tb.build()).build()).build();
	}

	private void createLink(final DataModification<InstanceIdentifier<?>, DataObject> trans, final String pfx, final LinkstateRoute value, final Link l, final Attributes attributes) {
		final LinkAttributes la = (LinkAttributes) attributes.getAugmentation(Attributes1.class).getAttributeType();

		final IgpLinkAttributesBuilder ilab = new IgpLinkAttributesBuilder();
		ilab.setMetric(la.getMetric().getValue());
		ilab.setName(la.getLinkName());

		switch (value.getProtocolId()) {
		case Direct:
		case Static:
		case Unknown:
			break;
		case IsisLevel1:
		case IsisLevel2:
			ilab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1.class, isisLinkAttributes(l.getLinkDescriptors().getMultiTopologyId(), la));
			break;
		case Ospf:
			ilab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1.class, ospfLinkAttributes(l.getLinkDescriptors().getMultiTopologyId(), la));
			break;
		}

		final LinkBuilder lb = new LinkBuilder();
		lb.setLinkId(buildLinkId(pfx, l));
		lb.addAugmentation(Link1.class, new Link1Builder().setIgpLinkAttributes(ilab.build()).build());

		// FIXME: figure this out
		lb.setSource(null);
		lb.setDestination(null);

		trans.putOperationalData(buildLinkIdentifier(pfx, l), lb.build());
	}

	private void removeLink(final DataModification<InstanceIdentifier<?>, DataObject> trans,  final String pfx, final Link l) {
		trans.removeOperationalData(buildLinkIdentifier(pfx, l));
	}

	private InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nodeIdentifierBuilder(final String pfx, final NodeIdentifier node) {
		return InstanceIdentifier.builder(getTopology()).node(
				org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
				new NodeKey(buildNodeId(pfx, node)));
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1 isisNodeAttributes(final NodeIdentifier node, final NodeAttributes na) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder tb =
				new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder();
		if (na.getIpv4RouterId() != null) {
			tb.setTeRouterIdIpv4(na.getIpv4RouterId());
		}
		if (na.getIpv6RouterId() != null) {
			tb.setTeRouterIdIpv6(na.getIpv6RouterId());
		}

		final IsisNodeAttributesBuilder ab = new IsisNodeAttributesBuilder();
		final CRouterIdentifier ri = node.getCRouterIdentifier();

		if (ri instanceof IsisPseudonode) {
			final IsisPseudonode pn = (IsisPseudonode) ri;
			ab.setIso(new IsoBuilder().setIsoPseudonodeId(new IsoPseudonodeId(pn.toString())).build());
		} else if (ri instanceof IsisNode) {
			final IsisNode in = (IsisNode) ri;
			ab.setIso(new IsoBuilder().setIsoSystemId(new IsoSystemId(in.getIsoSystemId().toString())).build());
		}

		ab.setTed(tb.build());

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder().setIsisNodeAttributes(ab.build()).build();
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1 ospfNodeAttributes(final NodeAttributes na) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder tb =
				new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder();

		final OspfNodeAttributesBuilder ab = new OspfNodeAttributesBuilder();

		ab.setTed(tb.build());

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1Builder().setOspfNodeAttributes(ab.build()).build();
	}

	private void createNode(final DataModification<InstanceIdentifier<?>, DataObject> trans, final String pfx, final LinkstateRoute value, final Node n, final Attributes attributes) {
		final NodeAttributes na = (NodeAttributes) attributes.getAugmentation(Attributes1.class).getAttributeType();


		final List<IpAddress> ids = new ArrayList<>();
		if (na.getIpv4RouterId() != null) {
			ids.add(new IpAddress(na.getIpv4RouterId()));
		}
		if (na.getIpv6RouterId() != null) {
			ids.add(new IpAddress(na.getIpv6RouterId()));
		}

		final IgpNodeAttributesBuilder inab = new IgpNodeAttributesBuilder();
		if (!ids.isEmpty()) {
			inab.setRouterId(ids);
		}

		switch (value.getProtocolId()) {
		case Direct:
		case Static:
		case Unknown:
			break;
		case IsisLevel1:
		case IsisLevel2:
			inab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1.class, isisNodeAttributes(n.getNodeDescriptors(), na));
			break;
		case Ospf:
			inab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1.class, ospfNodeAttributes(na));
			break;
		}

		final NodeBuilder nb = new NodeBuilder();
		nb.setNodeId(buildNodeId(pfx, n.getNodeDescriptors()));
		nb.addAugmentation(Node1.class, new Node1Builder().setIgpNodeAttributes(
				inab.build()).build());

		trans.putOperationalData(nodeIdentifierBuilder(pfx, n.getNodeDescriptors()).toInstance(), nb.build());
	}

	private void removeNode(final DataModification<InstanceIdentifier<?>, DataObject> trans, final String pfx, final Node n) {
		trans.removeOperationalData(nodeIdentifierBuilder(pfx, n.getNodeDescriptors()).toInstance());
	}

	private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix>
	prefixIdentifier(final String pfx, final Prefix p) {
		return nodeIdentifierBuilder(pfx, p.getAdvertisingNodeDescriptors()).node(
				Node1.class).node(IgpNodeAttributes.class).node(
						org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix.class,
						new PrefixKey(p.getIpReachabilityInformation())).toInstance();
	}

	private void createPrefix(final DataModification<InstanceIdentifier<?>, DataObject> trans, final String pfx, final LinkstateRoute value, final Prefix p, final Attributes attributes) {
		final PrefixAttributes pa = (PrefixAttributes) attributes.getAugmentation(Attributes1.class).getAttributeType();

		final PrefixBuilder pb = new PrefixBuilder();
		pb.setPrefix(p.getIpReachabilityInformation());
		pb.setMetric(pa.getPrefixMetric().getValue());

		switch (value.getProtocolId()) {
		case Direct:
		case IsisLevel1:
		case IsisLevel2:
		case Static:
		case Unknown:
			break;
		case Ospf:
			pb.addAugmentation(Prefix1.class, new Prefix1Builder().setOspfPrefixAttributes(
					new OspfPrefixAttributesBuilder().setForwardingAddress(pa.getOspfForwardingAddress()).build()).build());
			break;
		}

		trans.putOperationalData(prefixIdentifier(pfx, p), pb.build());
	}

	private void removePrefix(final DataModification<InstanceIdentifier<?>, DataObject> trans, final String pfx, final Prefix p) {
		trans.removeOperationalData(prefixIdentifier(pfx, p));
	}

	@Override
	protected void createObject(final DataModification<InstanceIdentifier<?>, DataObject> trans, final InstanceIdentifier<LinkstateRoute> id, final LinkstateRoute value) {
		final String pfx = buildNamePrefix(value);

		final ObjectType t = value.getObjectType();
		if (t instanceof Link) {
			createLink(trans, pfx, value, (Link)t, value.getAttributes());
		} else if (t instanceof Node) {
			createNode(trans, pfx, value, (Node)t, value.getAttributes());
		} else if (t instanceof Prefix) {
			createPrefix(trans, pfx, value, (Prefix)t, value.getAttributes());
		} else {
			throw new IllegalStateException("Unhandled object class " + t.getImplementedInterface());
		}
	}

	@Override
	protected void removeObject(final DataModification<InstanceIdentifier<?>, DataObject> trans, final InstanceIdentifier<LinkstateRoute> id, final LinkstateRoute value) {
		final String pfx = buildNamePrefix(value);

		final ObjectType t = value.getObjectType();
		if (t instanceof Link) {
			removeLink(trans, pfx, (Link)t);
		} else if (t instanceof Node) {
			removeNode(trans, pfx, (Node)t);
		} else if (t instanceof Prefix) {
			removePrefix(trans, pfx, (Prefix)t);
		} else {
			throw new IllegalStateException("Unhandled object class " + t.getImplementedInterface());
		}
	}
}
