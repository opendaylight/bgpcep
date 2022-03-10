/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoNetId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoPseudonodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.IsoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.srlg.attributes.SrlgValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.OspfLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.OspfNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.AbrBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.InternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.PseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.prefix.attributes.OspfPrefixAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class ProtocolUtil {
    private static final Decimal64 MIN_BANDWIDTH = Decimal64.minValueIn(2);
    private static final float MIN_BANDWIDTH_FLT = MIN_BANDWIDTH.floatValue();
    private static final Decimal64 MAX_BANDWIDTH = Decimal64.maxValueIn(2);
    private static final float MAX_BANDWIDTH_FLT = MAX_BANDWIDTH.floatValue();

    private ProtocolUtil() {
        // Hidden on purpose
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
                    pb.addAugmentation(new Prefix1Builder().setOspfPrefixAttributes(
                            new OspfPrefixAttributesBuilder().setForwardingAddress(pa.getOspfForwardingAddress()
                                    .getIpv4AddressNoZone()).build()).build());
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
                inab.addAugmentation(isisNodeAttributes(nd, na));
                break;
            case Ospf:
                inab.addAugmentation(ospfNodeAttributes(nd, na));
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
                ilab.addAugmentation(isisLinkAttributes(ld.getMultiTopologyId(), la));
                break;
            case OspfV3:
            case Ospf:
                ilab.addAugmentation(ospfLinkAttributes(ld.getMultiTopologyId(), la));
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

                ab.setRouterType(new PseudonodeBuilder().setPseudonode(Empty.value()).build());
                ab.setDrInterfaceId(pn.getLanInterface().getValue());
            } else if (ri instanceof OspfNodeCase && na.getNodeFlags() != null) {
                // TODO: what should we do with in.getOspfRouterId()?

                final NodeFlagBits nf = na.getNodeFlags();
                if (nf.getAbr() != null) {
                    ab.setRouterType(new AbrBuilder().setAbr(nf.getAbr() ? Empty.value() : null).build());
                } else if (nf.getExternal() != null) {
                    ab.setRouterType(new InternalBuilder().setInternal(nf.getExternal() ? null : Empty.value())
                        .build());
                }
            }
        }
        ab.setTed(tb.build());
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021
                .IgpNodeAttributes1Builder().setOspfNodeAttributes(ab.build()).build();
    }


    private static Set<IsoNetId> toIsoNetIds(final Set<IsisAreaIdentifier> areaIds, final String systemId) {
        return areaIds.stream().map(input -> new IsoNetId(UriBuilder.toIsoNetId(input, systemId)))
                .collect(ImmutableSet.toImmutableSet());
    }

    private static Set<Uint8> nodeMultiTopology(final Set<TopologyIdentifier> list) {
        final var builder = ImmutableSet.<Uint8>builderWithExpectedSize(list.size());
        for (final TopologyIdentifier id : list) {
            builder.add(Uint8.valueOf(id.getValue()));
        }
        return builder.build();
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
                tb.setUnreservedBandwidth(unreservedBandwidthList(la.nonnullUnreservedBandwidth().values()));
            }
            if (la.getMaxLinkBandwidth() != null) {
                tb.setMaxLinkBandwidth(bandwidthToDecimal64(la.getMaxLinkBandwidth()));
            }
            if (la.getMaxReservableBandwidth() != null) {
                tb.setMaxResvLinkBandwidth(bandwidthToDecimal64(la.getMaxReservableBandwidth()));
            }
            if (la.getSharedRiskLinkGroups() != null) {
                tb.setSrlg(new SrlgBuilder()
                    .setSrlgValues(la.getSharedRiskLinkGroups().stream()
                        .map(id -> new SrlgValuesBuilder().setSrlgValue(id.getValue()).build())
                        .collect(BindingMap.toOrderedMap()))
                    .build());
            }
        }

        final OspfLinkAttributesBuilder ilab = new OspfLinkAttributesBuilder();
        ilab.setTed(tb.build());
        if (topologyIdentifier != null) {
            ilab.setMultiTopologyId(Uint8.valueOf(topologyIdentifier.getValue()));
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
                tb.setUnreservedBandwidth(unreservedBandwidthList(la.nonnullUnreservedBandwidth().values()));
            }
            if (la.getMaxLinkBandwidth() != null) {
                tb.setMaxLinkBandwidth(bandwidthToDecimal64(la.getMaxLinkBandwidth()));
            }
            if (la.getMaxReservableBandwidth() != null) {
                tb.setMaxResvLinkBandwidth(bandwidthToDecimal64(la.getMaxReservableBandwidth()));
            }
            if (la.getSharedRiskLinkGroups() != null) {
                tb.setSrlg(new SrlgBuilder()
                    .setSrlgValues(la.getSharedRiskLinkGroups().stream()
                        .map(id -> new SrlgValuesBuilder().setSrlgValue(id.getValue()).build())
                        .collect(BindingMap.toOrderedMap()))
                    .build());
            }
        }

        final IsisLinkAttributesBuilder ilab = new IsisLinkAttributesBuilder();
        ilab.setTed(tb.build());
        if (topologyIdentifier != null) {
            ilab.setMultiTopologyId(Uint8.valueOf(topologyIdentifier.getValue()));
        }

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021
                .IgpLinkAttributes1Builder().setIsisLinkAttributes(ilab.build()).build();
    }

    static Decimal64 bandwidthToDecimal64(final Bandwidth bandwidth) {
        final float floatValue = ByteBuffer.wrap(bandwidth.getValue()).getFloat();
        if (floatValue <= MIN_BANDWIDTH_FLT) {
            return MIN_BANDWIDTH;
        }
        if (floatValue >= MAX_BANDWIDTH_FLT) {
            return MAX_BANDWIDTH;
        }

        // Deal with rounding...
        return Decimal64.of(2, BigDecimal.valueOf(floatValue)
            .setScale(2, RoundingMode.HALF_UP)
            .unscaledValue()
            .longValueExact());
    }

    private static Map<UnreservedBandwidthKey, UnreservedBandwidth> unreservedBandwidthList(
            final Collection<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .bgp.linkstate.rev200120.UnreservedBandwidth> input) {
        return input.stream()
            .map(bandwidth -> new UnreservedBandwidthBuilder()
                .setBandwidth(bandwidthToDecimal64(bandwidth.getBandwidth()))
                .withKey(new UnreservedBandwidthKey(bandwidth.getPriority()))
                .build())
            .collect(BindingMap.toOrderedMap());
    }
}
