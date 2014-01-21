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
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.link._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.node._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.prefix._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.node._case.OspfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.IgpTerminationPointAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.OspfLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.OspfNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.AbrBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.InternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.PseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.prefix.attributes.OspfPrefixAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedInteger;

public final class LinkstateTopologyBuilder extends AbstractTopologyBuilder<LinkstateRoute> {
	private static final Logger LOG = LoggerFactory.getLogger(LinkstateTopologyBuilder.class);

	public LinkstateTopologyBuilder(final DataProviderService dataProvider, final RibReference locRibReference, final TopologyId topologyId) {
		super(dataProvider, locRibReference, topologyId, new TopologyTypesBuilder().addAugmentation(TopologyTypes1.class,
				new TopologyTypes1Builder().build()).build(), LinkstateRoute.class);
	}

	private LinkId buildLinkId(final UriBuilder base, final LinkCase link) {
		return new LinkId(new UriBuilder(base, "link").add(link).toString());
	}

	private NodeId buildNodeId(final UriBuilder base, final NodeIdentifier node) {
		return new NodeId(new UriBuilder(base, "node").add("", node).toString());
	}

	private TpId buildTpId(final UriBuilder base, final TopologyIdentifier topologyIdentifier,
			final Ipv4InterfaceIdentifier ipv4InterfaceIdentifier, final Ipv6InterfaceIdentifier ipv6InterfaceIdentifier, final byte[] bs) {
		return new TpId(new UriBuilder(base, "tp").add("mt", topologyIdentifier).add("ipv4", ipv4InterfaceIdentifier).add("ipv6",
				ipv6InterfaceIdentifier).add("id", bs).toString());
	}

	private TpId buildLocalTpId(final UriBuilder base, final LinkDescriptors linkDescriptors) {
		return buildTpId(base, linkDescriptors.getMultiTopologyId(), linkDescriptors.getIpv4InterfaceAddress(),
				linkDescriptors.getIpv6InterfaceAddress(), linkDescriptors.getLinkLocalIdentifier());
	}

	private TerminationPoint buildTp(final TpId id, final TerminationPointType type) {
		final TerminationPointBuilder stpb = new TerminationPointBuilder();
		stpb.setKey(new TerminationPointKey(id));
		stpb.setTpId(id);

		if (type != null) {
			stpb.addAugmentation(
					TerminationPoint1.class,
					new TerminationPoint1Builder().setIgpTerminationPointAttributes(
							new IgpTerminationPointAttributesBuilder().setTerminationPointType(null).build()).build());
		}

		return stpb.build();
	}

	private TerminationPointType getTpType(final Ipv4InterfaceIdentifier ipv4InterfaceIdentifier,
			final Ipv6InterfaceIdentifier ipv6InterfaceIdentifier, final byte[] bs) {
		// Order of preference: Unnumbered first, then IP
		if (bs != null) {
			final long id = UnsignedInteger.fromIntBits(ByteArray.bytesToInt(bs)).longValue();
			LOG.debug("Unnumbered termination point type: {}", id);
			return new UnnumberedBuilder().setUnnumberedId(id).build();
		}

		final IpAddress ip;
		if (ipv6InterfaceIdentifier != null) {
			ip = new IpAddress(ipv6InterfaceIdentifier);
		} else if (ipv4InterfaceIdentifier != null) {
			ip = new IpAddress(ipv4InterfaceIdentifier);
		} else {
			ip = null;
		}

		if (ip != null) {
			LOG.debug("IP termination point type: {}", ip);
			return new IpBuilder().setIpAddress(Lists.newArrayList(ip)).build();
		}

		return null;
	}

	private TerminationPoint buildLocalTp(final UriBuilder base, final LinkDescriptors linkDescriptors) {
		final TpId id = buildLocalTpId(base, linkDescriptors);
		final TerminationPointType t = getTpType(linkDescriptors.getIpv4InterfaceAddress(), linkDescriptors.getIpv6InterfaceAddress(),
				linkDescriptors.getLinkLocalIdentifier());

		return buildTp(id, t);
	}

	private TpId buildRemoteTpId(final UriBuilder base, final LinkDescriptors linkDescriptors) {
		return buildTpId(base, linkDescriptors.getMultiTopologyId(), linkDescriptors.getIpv4NeighborAddress(),
				linkDescriptors.getIpv6NeighborAddress(), linkDescriptors.getLinkRemoteIdentifier());
	}

	private TerminationPoint buildRemoteTp(final UriBuilder base, final LinkDescriptors linkDescriptors) {
		final TpId id = buildRemoteTpId(base, linkDescriptors);
		final TerminationPointType t = getTpType(linkDescriptors.getIpv4NeighborAddress(), linkDescriptors.getIpv6NeighborAddress(),
				linkDescriptors.getLinkRemoteIdentifier());

		return buildTp(id, t);
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

		if (la != null) {
			if (la.getAdminGroup() != null) {
				tb.setColor(la.getAdminGroup().getValue());
			}
			if (la.getTeMetric() != null) {
				tb.setTeDefaultMetric(la.getTeMetric().getValue());
			}
			if (la.getUnreservedBandwidth() != null) {
				tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
			}
			if (la.getMaxLinkBandwidth() != null) {
				tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
			}
			if (la.getMaxReservableBandwidth() != null) {
				tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));
			}
		}

		final IsisLinkAttributesBuilder ilab = new IsisLinkAttributesBuilder();
		ilab.setTed(tb.build());
		if (topologyIdentifier != null) {
			ilab.setMultiTopologyId(topologyIdentifier.getValue().shortValue());
		}

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder().setIsisLinkAttributes(
				ilab.build()).build();
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1 ospfLinkAttributes(
			final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder();

		if (la != null) {
			if (la.getAdminGroup() != null) {
				tb.setColor(la.getAdminGroup().getValue());
			}
			if (la.getTeMetric() != null) {
				tb.setTeDefaultMetric(la.getTeMetric().getValue());
			}
			if (la.getUnreservedBandwidth() != null) {
				tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
			}
			if (la.getMaxLinkBandwidth() != null) {
				tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
			}
			if (la.getMaxReservableBandwidth() != null) {
				tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));
			}
		}

		final OspfLinkAttributesBuilder ilab = new OspfLinkAttributesBuilder();
		ilab.setTed(tb.build());
		if (topologyIdentifier != null) {
			ilab.setMultiTopologyId(topologyIdentifier.getValue().shortValue());
		}

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1Builder().setOspfLinkAttributes(
				ilab.build()).build();
	}

	private void createLink(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base,
			final LinkstateRoute value, final LinkCase l, final Attributes attributes) {
		final LinkAttributes la = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.LinkCase) attributes.getAugmentation(
				Attributes1.class).getAttributeType()).getLinkAttributes();

		final IgpLinkAttributesBuilder ilab = new IgpLinkAttributesBuilder();
		if (la != null) {
			ilab.setMetric(la.getMetric().getValue());
			ilab.setName(la.getLinkName());
		}

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

		final NodeId srcNode = buildNodeId(base, l.getLocalNodeDescriptors());
		final NodeId dstNode = buildNodeId(base, l.getRemoteNodeDescriptors());
		final TerminationPoint srcTp = buildLocalTp(base, l.getLinkDescriptors());
		final TerminationPoint dstTp = buildRemoteTp(base, l.getLinkDescriptors());

		lb.setSource(new SourceBuilder().setSourceNode(srcNode).setSourceTp(srcTp.getTpId()).build());
		lb.setDestination(new DestinationBuilder().setDestNode(dstNode).setDestTp(dstTp.getTpId()).build());

		final InstanceIdentifier<TerminationPoint> stpId = buildTpIdentifier(srcNode, srcTp.getKey());
		trans.putOperationalData(stpId, srcTp);
		LOG.debug("Created TP {} at {} as link source", srcTp, stpId);

		final InstanceIdentifier<TerminationPoint> dtpId = buildTpIdentifier(dstNode, dstTp.getKey());
		trans.putOperationalData(dtpId, dstTp);
		LOG.debug("Created TP {} at {} as link destination", dstTp, dtpId);

		final InstanceIdentifier<?> lid = buildLinkIdentifier(base, l);
		final Link link = lb.build();
		trans.putOperationalData(lid, link);
		LOG.debug("Created link {} at {} for {}", link, lid, l);
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

	private InstanceIdentifier<TerminationPoint> buildTpIdentifier(final NodeId node, final TerminationPointKey key) {
		return InstanceIdentifier.builder(getInstanceIdentifier()).child(
				org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
				new NodeKey(node)).child(TerminationPoint.class, key).build();
	}

	private List<Short> nodeMultiTopology(final List<TopologyIdentifier> list) {
		final List<Short> ret = new ArrayList<>(list.size());
		for (final TopologyIdentifier id : list) {
			ret.add(id.getValue().shortValue());
		}
		return ret;
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1 isisNodeAttributes(
			final NodeIdentifier node, final NodeAttributes na) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder();
		final IsisNodeAttributesBuilder ab = new IsisNodeAttributesBuilder();

		if (na != null) {
			if (na.getIpv4RouterId() != null) {
				tb.setTeRouterIdIpv4(na.getIpv4RouterId());
			}
			if (na.getIpv6RouterId() != null) {
				tb.setTeRouterIdIpv6(na.getIpv6RouterId());
			}
			if (na.getTopologyIdentifier() != null) {
				ab.setMultiTopologyId(nodeMultiTopology(na.getTopologyIdentifier()));
			}
		}

		final CRouterIdentifier ri = node.getCRouterIdentifier();
		if (ri instanceof IsisPseudonodeCase) {
			final IsisPseudonode pn = ((IsisPseudonodeCase) ri).getIsisPseudonode();
			ab.setIso(new IsoBuilder().setIsoPseudonodeId(new IsoPseudonodeId(pn.toString())).build());
		} else if (ri instanceof IsisNodeCase) {
			final IsisNode in = ((IsisNodeCase) ri).getIsisNode();
			ab.setIso(new IsoBuilder().setIsoSystemId(new IsoSystemId(in.getIsoSystemId().toString())).build());
		}

		ab.setTed(tb.build());

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder().setIsisNodeAttributes(
				ab.build()).build();
	}

	private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1 ospfNodeAttributes(
			final NodeDescriptors node, final NodeAttributes na) {
		final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder();
		final OspfNodeAttributesBuilder ab = new OspfNodeAttributesBuilder();

		if (na != null) {
			if (na.getIpv4RouterId() != null) {
				tb.setTeRouterIdIpv4(na.getIpv4RouterId());
			}
			if (na.getIpv6RouterId() != null) {
				tb.setTeRouterIdIpv6(na.getIpv6RouterId());
			}
			if (na.getTopologyIdentifier() != null) {
				ab.setMultiTopologyId(nodeMultiTopology(na.getTopologyIdentifier()));
			}
		}

		final CRouterIdentifier ri = node.getCRouterIdentifier();
		if (ri instanceof OspfPseudonodeCase) {
			final OspfPseudonode pn = ((OspfPseudonodeCase) ri).getOspfPseudonode();

			ab.setRouterType(new PseudonodeBuilder().setPseudonode(Boolean.TRUE).build());
			ab.setDrInterfaceId(UnsignedInteger.fromIntBits(ByteArray.bytesToInt(pn.getLanInterface().getValue())).longValue());
		} else if (ri instanceof OspfNodeCase) {
			final OspfNode in = ((OspfNodeCase) ri).getOspfNode();

			// TODO: what should we do with in.getOspfRouterId()?

			if (na != null) {
				final NodeFlagBits nf = na.getNodeFlags();
				if (nf != null) {
					if (nf.isAbr()) {
						ab.setRouterType(new AbrBuilder().setAbr(Boolean.TRUE).build());
					} else if (!nf.isExternal()) {
						ab.setRouterType(new InternalBuilder().setInternal(Boolean.TRUE).build());
					}
				}
			}
		}

		ab.setTed(tb.build());

		return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1Builder().setOspfNodeAttributes(
				ab.build()).build();
	}

	private void createNode(final DataModification<InstanceIdentifier<?>, DataObject> trans, final UriBuilder base,
			final LinkstateRoute value, final NodeCase n, final Attributes attributes) {
		final NodeAttributes na = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.NodeCase) attributes.getAugmentation(
				Attributes1.class).getAttributeType()).getNodeAttributes();
		final IgpNodeAttributesBuilder inab = new IgpNodeAttributesBuilder();

		final List<IpAddress> ids = new ArrayList<>();
		if (na != null) {
			if (na.getIpv4RouterId() != null) {
				ids.add(new IpAddress(na.getIpv4RouterId()));
			}
			if (na.getIpv6RouterId() != null) {
				ids.add(new IpAddress(na.getIpv6RouterId()));
			}
			if (na.getDynamicHostname() != null) {
				inab.setName(new DomainName(na.getDynamicHostname()));
			}
		}

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
					ospfNodeAttributes(n.getNodeDescriptors(), na));
			break;
		}

		final NodeBuilder nb = new NodeBuilder();
		nb.setNodeId(buildNodeId(base, n.getNodeDescriptors()));
		nb.addAugmentation(Node1.class, new Node1Builder().setIgpNodeAttributes(inab.build()).build());

		final InstanceIdentifier<Node> nid = nodeIdentifierBuilder(base, n.getNodeDescriptors()).build();
		final Node node = nb.build();
		trans.putOperationalData(nid, node);
		LOG.debug("Created node {} at {} for {}", node, nid, n);
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
		final PrefixAttributes pa = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.bgp.rib.rib.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.PrefixCase) attributes.getAugmentation(
				Attributes1.class).getAttributeType()).getPrefixAttributes();

		final PrefixBuilder pb = new PrefixBuilder();
		final IpPrefix ippfx = p.getIpReachabilityInformation();
		pb.setKey(new PrefixKey(ippfx));
		pb.setPrefix(ippfx);
		if (pa != null) {
			pb.setMetric(pa.getPrefixMetric().getValue());
		}

		switch (value.getProtocolId()) {
		case Direct:
		case IsisLevel1:
		case IsisLevel2:
		case Static:
		case Unknown:
			break;
		case Ospf:
			if (pa != null) {
				pb.addAugmentation(
						Prefix1.class,
						new Prefix1Builder().setOspfPrefixAttributes(
								new OspfPrefixAttributesBuilder().setForwardingAddress(pa.getOspfForwardingAddress()).build()).build());
			}
			break;
		}


		final Prefix pfx = pb.build();
		final InstanceIdentifier<Prefix> pid = prefixIdentifier(base, p);
		trans.putOperationalData(pid, pfx);
		LOG.debug("Created prefix {} at {} for {}", pfx, pid, p);
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
