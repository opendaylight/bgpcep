/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoNetId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoPseudonodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.IsoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.srlg.attributes.SrlgValues;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.srlg.attributes.SrlgValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.OspfLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.OspfNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.AbrBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.InternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.PseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.prefix.attributes.OspfPrefixAttributesBuilder;

public final class ProtocolUtil {
    private ProtocolUtil() {
        throw new UnsupportedOperationException();
    }

    public static void augmentProtocolId(final LinkstateRoute value, final PrefixAttributes pa,
            final PrefixBuilder pb) {
        switch (value.getProtocolId()) {
            case Direct:
            case IsisLevel1:
            case IsisLevel2:
            case Static:
            case Ospf:
                if (pa != null && pa.getOspfForwardingAddress() != null) {
                    pb.addAugmentation(Prefix1.class, new Prefix1Builder().setOspfPrefixAttributes(
                            new OspfPrefixAttributesBuilder().setForwardingAddress(pa.getOspfForwardingAddress()
                                    .getIpv4Address()).build()).build());
                }
                break;
            default:
                break;
        }
    }

    public static void augmentProtocolId(final LinkstateRoute value, final IgpNodeAttributesBuilder inab,
            final NodeAttributes na, final NodeIdentifier nd) {
        switch (value.getProtocolId()) {
            case Direct:
            case Static:
            case IsisLevel1:
            case IsisLevel2:
                inab.addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
                                .IgpNodeAttributes1.class, isisNodeAttributes(nd, na));
                break;
            case Ospf:
                inab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
                        .IgpNodeAttributes1.class, ospfNodeAttributes(nd, na));
                break;
            default:
                break;
        }
    }

    public static void augmentProtocolId(final LinkstateRoute value, final IgpLinkAttributesBuilder ilab,
            final LinkAttributes la, final LinkDescriptors ld) {
        switch (value.getProtocolId()) {
            case Direct:
            case Static:
            case IsisLevel1:
            case IsisLevel2:
                ilab.addAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
                                .IgpLinkAttributes1.class, isisLinkAttributes(ld.getMultiTopologyId(), la));
                break;
            case OspfV3:
            case Ospf:
                ilab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
                        .IgpLinkAttributes1.class, ospfLinkAttributes(ld.getMultiTopologyId(), la));
                break;
            default:
                break;
        }
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
            .IgpNodeAttributes1 isisNodeAttributes(final NodeIdentifier node, final NodeAttributes na) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node
                .attributes.isis.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml
                .ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder();
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
            final IsoBuilder b = new IsoBuilder();
            final String systemId = UriBuilder.isoId(pn.getIsIsRouterIdentifier().getIsoSystemId());
            b.setIsoSystemId(new IsoSystemId(systemId));
            b.setIsoPseudonodeId(new IsoPseudonodeId(BaseEncoding.base16()
                    .encode(new byte[]{pn.getPsn().byteValue()})));
            ab.setIso(b.build());
            if (na != null) {
                ab.setNet(toIsoNetIds(na.getIsisAreaId(), systemId));
            }
        } else if (ri instanceof IsisNodeCase) {
            final IsisNode in = ((IsisNodeCase) ri).getIsisNode();
            final String systemId = UriBuilder.isoId(in.getIsoSystemId());
            ab.setIso(new IsoBuilder().setIsoSystemId(new IsoSystemId(systemId)).build());
            if (na != null) {
                ab.setNet(toIsoNetIds(na.getIsisAreaId(), systemId));
            }
        }

        ab.setTed(tb.build());

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
                .IgpNodeAttributes1Builder().setIsisNodeAttributes(ab.build()).build();
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
            .IgpNodeAttributes1 ospfNodeAttributes(
            final NodeIdentifier node, final NodeAttributes na) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node
                .attributes.ospf.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd
                .params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder();
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
            final CRouterIdentifier ri = node.getCRouterIdentifier();
            if (ri instanceof OspfPseudonodeCase) {
                final OspfPseudonode pn = ((OspfPseudonodeCase) ri).getOspfPseudonode();

                ab.setRouterType(new PseudonodeBuilder().setPseudonode(Boolean.TRUE).build());
                ab.setDrInterfaceId(pn.getLanInterface().getValue());
            } else if (ri instanceof OspfNodeCase && na.getNodeFlags() != null) {
                // TODO: what should we do with in.getOspfRouterId()?

                final NodeFlagBits nf = na.getNodeFlags();
                if (nf.isAbr() != null) {
                    ab.setRouterType(new AbrBuilder().setAbr(nf.isAbr()).build());
                } else if (nf.isExternal() != null) {
                    ab.setRouterType(new InternalBuilder().setInternal(!nf.isExternal()).build());
                }
            }
        }
        ab.setTed(tb.build());
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
                .IgpNodeAttributes1Builder().setOspfNodeAttributes(ab.build()).build();
    }


    private static List<IsoNetId> toIsoNetIds(final List<IsisAreaIdentifier> areaIds, final String systemId) {
        return Lists.transform(areaIds, input -> new IsoNetId(UriBuilder.toIsoNetId(input, systemId)));
    }

    private static List<Short> nodeMultiTopology(final List<TopologyIdentifier> list) {
        final List<Short> ret = new ArrayList<>(list.size());
        for (final TopologyIdentifier id : list) {
            ret.add(id.getValue().shortValue());
        }
        return ret;
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
            .IgpLinkAttributes1 ospfLinkAttributes(final TopologyIdentifier topologyIdentifier,
            final LinkAttributes la) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes
                .ospf.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                .ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder();

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
            if (la.getSharedRiskLinkGroups() != null) {
                final List<SrlgValues> srlgs = new ArrayList<>();
                for (final SrlgId id : la.getSharedRiskLinkGroups()) {
                    srlgs.add(new SrlgValuesBuilder().setSrlgValue(id.getValue()).build());
                }
                tb.setSrlg(new SrlgBuilder().setSrlgValues(srlgs).build());
            }
        }

        final OspfLinkAttributesBuilder ilab = new OspfLinkAttributesBuilder();
        ilab.setTed(tb.build());
        if (topologyIdentifier != null) {
            ilab.setMultiTopologyId(topologyIdentifier.getValue().shortValue());
        }

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
                .IgpLinkAttributes1Builder().setOspfLinkAttributes(ilab.build()).build();
    }


    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
            .IgpLinkAttributes1 isisLinkAttributes(
            final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis
                .link.attributes.isis.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params
                .xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder();

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
            if (la.getSharedRiskLinkGroups() != null) {
                final List<SrlgValues> srlgs = new ArrayList<>();
                for (final SrlgId id : la.getSharedRiskLinkGroups()) {
                    srlgs.add(new SrlgValuesBuilder().setSrlgValue(id.getValue()).build());
                }
                tb.setSrlg(new SrlgBuilder().setSrlgValues(srlgs).build());
            }
        }

        final IsisLinkAttributesBuilder ilab = new IsisLinkAttributesBuilder();
        ilab.setTed(tb.build());
        if (topologyIdentifier != null) {
            ilab.setMultiTopologyId(topologyIdentifier.getValue().shortValue());
        }

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
                .IgpLinkAttributes1Builder().setIsisLinkAttributes(ilab.build()).build();
    }

    private static Float bandwidthToFloat(final Bandwidth bandwidth) {
        return ByteBuffer.wrap(bandwidth.getValue()).getFloat();
    }

    private static BigDecimal bandwidthToBigDecimal(final Bandwidth bandwidth) {
        return BigDecimal.valueOf(bandwidthToFloat(bandwidth));
    }

    private static List<UnreservedBandwidth> unreservedBandwidthList(
            final List<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate
                    .rev171207.UnreservedBandwidth> input) {
        final List<UnreservedBandwidth> ret = new ArrayList<>(input.size());

        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207
                .UnreservedBandwidth bandwidth : input) {
            ret.add(new UnreservedBandwidthBuilder().setBandwidth(bandwidthToBigDecimal(bandwidth.getBandwidth()))
                    .setKey(new UnreservedBandwidthKey(bandwidth.getPriority())).build());
        }

        return ret;
    }
}
