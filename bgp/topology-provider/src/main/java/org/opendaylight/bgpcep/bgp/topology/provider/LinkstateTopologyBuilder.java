/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.LocRibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
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

public final class LinkstateTopologyBuilder extends AbstractTopologyBuilder<LinkstateRoute> {
	private static final class UriBuilder {
		private final StringBuilder sb;

		UriBuilder(final UriBuilder base, final String type) {
			sb = new StringBuilder(base.sb);
			sb.append("type=").append(type);
		}

		UriBuilder(final LinkstateRoute route) {
			sb = new StringBuilder("bgpls://");

			if (route.getDistinguisher() != null) {
				sb.append(route.getDistinguisher().getValue().toString()).append(':');
			}

			sb.append(route.getProtocolId().toString()).append(':').append(route.getIdentifier().getValue().toString()).append('/');
		}

		UriBuilder add(final String name, final Object value) {
			if (value != null) {
				sb.append('&').append(name).append('=').append(value.toString());
			}
			return this;
		}

		UriBuilder add(final String prefix, final NodeIdentifier node) {
			add(prefix + "as", node.getAsNumber());
			add(prefix + "domain", node.getDomainId());
			add(prefix + "area", node.getAreaId());;
			add(prefix + "router", node.getCRouterIdentifier());
			return this;
		}

		@Override
		public final String toString() {
			return sb.toString();
		}
	}

	public LinkstateTopologyBuilder(final DataProviderService dataProvider, final LocRibReference locRibReference, final TopologyId topologyId) {
		super(dataProvider, locRibReference, topologyId, LinkstateRoute.class);
	}

	private LinkId buildLinkId(final UriBuilder base, final LinkCase link) {
		final UriBuilder ub = new UriBuilder(base, "link");

		ub.add("local-", link.getLocalNodeDescriptors());
		ub.add("remote-", link.getRemoteNodeDescriptors());

		final LinkDescriptors ld = link.getLinkDescriptors();
		ub.add("ipv4-iface", ld.getIpv4InterfaceAddress());
		ub.add("ipv4-neigh", ld.getIpv4NeighborAddress());
		ub.add("ipv6-iface", ld.getIpv6InterfaceAddress());
		ub.add("ipv6-neigh", ld.getIpv6NeighborAddress());
		ub.add("mt", ld.getMultiTopologyId());
		ub.add("local-id", ld.getLinkLocalIdentifier());
		ub.add("remote-id", ld.getLinkRemoteIdentifier());

		return new LinkId(ub.toString());
	}

	private NodeId buildNodeId(final UriBuilder base, final NodeIdentifier node) {
		return new NodeId(new UriBuilder(base, "node").add("", node).toString());
	}

	private InstanceIdentifier<?> buildLinkIdentifier(final UriBuilder base, final LinkCase l) {
		return InstanceIdentifier.builder(getInstanceIdentifier()).child(
				org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link.class,
				new LinkKey(buildLinkId(base, l))).toInstance();
	}

	private static Float bandwidthToFloat(final Bandwidth bandwidth) {
		return ByteBuffer.wrap(bandwidth.getValue()).getFloat();
	}

	private static BigDecimal bandwidthToBigDecimal(final Bandwidth bandwidth) {
		return BigDecimal.valueOf(bandwidthToFloat(bandwidth));
	}

	private static List<UnreservedBandwidth> unreservedBandwidthList(
			final List<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.UnreservedBandwidth> input) {
		final List<UnreservedBandwidth> ret = new ArrayList<>(input.size());

		for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.UnreservedBandwidth i : input) {
			ret.add(new UnreservedBandwidthBuilder().setBandwidth(bandwidthToBigDecimal(i.getBandwidth())).setKey(
					new UnreservedBandwidthKey(i.getPriority())).build());
		}

		return ret;
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1 isisLinkAttributes(
			final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder();

		tb.setColor(la.getAdminGroup().getValue());
		tb.setTeDefaultMetric(la.getTeMetric().getValue());
		tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
		tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
		tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder().setIsisLinkAttributes(
				new IsisLinkAttributesBuilder().setMultiTopologyId(topologyIdentifier.getValue().shortValue()).setTed(tb.build()).build()).build();
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1 ospfLinkAttributes(
			final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder();

		tb.setColor(la.getAdminGroup().getValue());
		tb.setTeDefaultMetric(la.getTeMetric().getValue());
		tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
		tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
		tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1Builder().setOspfLinkAttributes(
				new OspfLinkAttributesBuilder().setMultiTopologyId(topologyIdentifier.getValue().shortValue()).setTed(tb.build()).build()).build();
	}

	private void createLink(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base, final LinkstateRoute value,
			final LinkCase l, final Attributes attributes) {
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
			ilab.addAugmentation(
					org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1.class,
					isisLinkAttributes(l.getLinkDescriptors().getMultiTopologyId(), la));
			break;
		case Ospf:
			ilab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1.class,
					ospfLinkAttributes(l.getLinkDescriptors().getMultiTopologyId(), la));
			break;
		}

		final LinkBuilder lb = new LinkBuilder();
		lb.setLinkId(buildLinkId(base, l));
		lb.addAugmentation(Link1.class, new Link1Builder().setIgpLinkAttributes(ilab.build()).build());

		// FIXME: figure this out
		lb.setSource(null);
		lb.setDestination(null);

		trans.putOperationalData(buildLinkIdentifier(base, l), lb.build());
	}

	private void removeLink(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base, final LinkCase l) {
		trans.removeOperationalData(buildLinkIdentifier(base, l));
	}

	private InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> nodeIdentifierBuilder(
			final UriBuilder base, final NodeIdentifier node) {
		return InstanceIdentifier.builder(getInstanceIdentifier()).child(
				org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
				new NodeKey(buildNodeId(base, node)));
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1 isisNodeAttributes(
			final NodeIdentifier node, final NodeAttributes na) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder();
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

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder().setIsisNodeAttributes(
				ab.build()).build();
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1 ospfNodeAttributes(
			final NodeAttributes na) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder();

		final OspfNodeAttributesBuilder ab = new OspfNodeAttributesBuilder();

		ab.setTed(tb.build());

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1Builder().setOspfNodeAttributes(
				ab.build()).build();
	}

	private void createNode(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base, final LinkstateRoute value,
			final NodeCase n, final Attributes attributes) {
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
			inab.addAugmentation(
					org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1.class,
					isisNodeAttributes(n.getNodeDescriptors(), na));
			break;
		case Ospf:
			inab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1.class,
					ospfNodeAttributes(na));
			break;
		}

		final NodeBuilder nb = new NodeBuilder();
		nb.setNodeId(buildNodeId(base, n.getNodeDescriptors()));
		nb.addAugmentation(Node1.class, new Node1Builder().setIgpNodeAttributes(inab.build()).build());

		trans.putOperationalData(nodeIdentifierBuilder(base, n.getNodeDescriptors()).toInstance(), nb.build());
	}

	private void removeNode(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base, final NodeCase n) {
		trans.removeOperationalData(nodeIdentifierBuilder(base, n.getNodeDescriptors()).toInstance());
	}

	private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix> prefixIdentifier(
			final UriBuilder base, final PrefixCase p) {
		return nodeIdentifierBuilder(base, p.getAdvertisingNodeDescriptors()).augmentation(Node1.class).child(
				org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes.class).child(
						org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix.class,
						new PrefixKey(p.getIpReachabilityInformation())).toInstance();
	}

	private void createPrefix(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base,
			final LinkstateRoute value, final PrefixCase p, final Attributes attributes) {
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
			pb.addAugmentation(
					Prefix1.class,
					new Prefix1Builder().setOspfPrefixAttributes(
							new OspfPrefixAttributesBuilder().setForwardingAddress(pa.getOspfForwardingAddress()).build()).build());
			break;
		}

		trans.putOperationalData(prefixIdentifier(base, p), pb.build());
	}

	private void removePrefix(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base, final PrefixCase p) {
		trans.removeOperationalData(prefixIdentifier(base, p));
	}

	@Override
	protected void createObject(final DataModification<InstanceIdentifier<?>, DataObject> trans,
			final InstanceIdentifier<LinkstateRoute> id, final LinkstateRoute value) {
		final UriBuilder base = new UriBuilder(value);

		final ObjectType t = value.getObjectType();
		if (t instanceof LinkCase) {
			createLink(trans, base, value, (LinkCase) t, value.getAttributes());
		} else if (t instanceof NodeCase) {
			createNode(trans, base, value, (NodeCase) t, value.getAttributes());
		} else if (t instanceof PrefixCase) {
			createPrefix(trans, base, value, (PrefixCase) t, value.getAttributes());
		} else {
			throw new IllegalStateException("Unhandled object class " + t.getImplementedInterface());
		}
	}

	@Override
	protected void removeObject(final DataModification<InstanceIdentifier<?>, DataObject> trans,
			final InstanceIdentifier<LinkstateRoute> id, final LinkstateRoute value) {
		final UriBuilder base = new UriBuilder(value);

		final ObjectType t = value.getObjectType();
		if (t instanceof LinkCase) {
			removeLink(trans, base, (LinkCase) t);
		} else if (t instanceof NodeCase) {
			removeNode(trans, base, (NodeCase) t);
		} else if (t instanceof PrefixCase) {
			removePrefix(trans, base, (PrefixCase) t);
		} else {
			throw new IllegalStateException("Unhandled object class " + t.getImplementedInterface());
		}
	}
}
