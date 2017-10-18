/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.testtool;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.spec.common.As4SpecificCommon;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as._4.spec.common.As4SpecificCommonBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.LinkBandwidthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as._4.generic.spec.extended.community._case.As4GenericSpecExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as._4.route.origin.extended.community._case.As4RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as._4.route.target.extended.community._case.As4RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.link.bandwidth._case.LinkBandwidthExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.opaque.extended.community._case.OpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.ipv4._case.RouteOriginIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.ipv4._case.RouteTargetIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CommunitiesBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(CommunitiesBuilder.class);
    private static final As4SpecificCommon AS_4_COMMON = new As4SpecificCommonBuilder().setAsNumber(new AsNumber(20L)).setLocalAdministrator(100).build();
    private static final Ipv4Address IPV4 = new Ipv4Address("192.168.1.0");
    private static final byte[] BYTE = new byte[]{(byte) 0x4f, (byte) 0x70, (byte) 0x00, (byte) 0x00};
    private static final int LA = 4660;
    private static final ShortAsNumber SHORT_AS = new ShortAsNumber(20L);

    private CommunitiesBuilder() {
        throw new UnsupportedOperationException();
    }

    static List<ExtendedCommunities> createExtComm(final List<String> extCom) {
        final List<ExtendedCommunities> extendedCommunities = Lists.newArrayList();
        for (String ec : extCom) {
            ExtendedCommunity community = null;
            switch (ec) {
            case "as-4-generic-spec-extended-community":
                community = CommunitiesBuilder.as4GenSpecBuild();
                break;
            case "as-4-route-target-extended-community":
                community = CommunitiesBuilder.as4RTBuild();
                break;
            case "as-4-route-origin-extended-community":
                community = CommunitiesBuilder.as4ROBuild();
                break;
            case "route-origin":
                community = CommunitiesBuilder.rOBuild();
                break;
            case "route-target":
                community = CommunitiesBuilder.rTBuild();
                break;
            case "route-origin-extended-community":
                community = CommunitiesBuilder.rOECBuild();
                break;
            case "route-target-extended-community":
                community = CommunitiesBuilder.rTECBuild();
                break;
            case "link-bandwidth-extended-community":
                community = CommunitiesBuilder.linkBandBuild();
                break;
            case "opaque-extended-community":
                community = CommunitiesBuilder.opaqueBuild();
                break;
            case "inet4-specific-extended-community":
                community = CommunitiesBuilder.inet4Build();
                break;
            case "as-specific-extended-community":
                community = CommunitiesBuilder.asSpecBuild();
                break;
            default:
                LOG.debug("Not recognized Extended Community {}", ec);
                break;
            }
            extendedCommunities.add(new ExtendedCommunitiesBuilder().setTransitive(true).setExtendedCommunity(community).build());
        }
        return extendedCommunities;
    }

    private static ExtendedCommunity as4GenSpecBuild() {
        return new As4GenericSpecExtendedCommunityCaseBuilder()
            .setAs4GenericSpecExtendedCommunity(new As4GenericSpecExtendedCommunityBuilder()
                .setAs4SpecificCommon(AS_4_COMMON).build()).build();
    }

    private static ExtendedCommunity as4RTBuild() {
        return new As4RouteTargetExtendedCommunityCaseBuilder().setAs4RouteTargetExtendedCommunity(
            new As4RouteTargetExtendedCommunityBuilder().setAs4SpecificCommon(AS_4_COMMON).build()).build();
    }

    private static ExtendedCommunity as4ROBuild() {
        return new As4RouteOriginExtendedCommunityCaseBuilder().setAs4RouteOriginExtendedCommunity(
            new As4RouteOriginExtendedCommunityBuilder().setAs4SpecificCommon(AS_4_COMMON).build()).build();
    }

    private static ExtendedCommunity rTBuild() {
        return new RouteTargetIpv4CaseBuilder().setRouteTargetIpv4(
            new RouteTargetIpv4Builder().setGlobalAdministrator(IPV4).setLocalAdministrator(LA).build()).build();
    }

    private static ExtendedCommunity rOBuild() {
        return new RouteOriginIpv4CaseBuilder().setRouteOriginIpv4(
            new RouteOriginIpv4Builder().setGlobalAdministrator(IPV4).setLocalAdministrator(LA).build()).build();
    }

    private static ExtendedCommunity linkBandBuild() {
        return new LinkBandwidthCaseBuilder().setLinkBandwidthExtendedCommunity(new LinkBandwidthExtendedCommunityBuilder()
            .setBandwidth(new Bandwidth(new Float32(BYTE))).build()).build();
    }

    private static ExtendedCommunity rOECBuild() {
        return new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
            new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(SHORT_AS).setLocalAdministrator(BYTE).build()).build();
    }

    private static ExtendedCommunity rTECBuild() {
        return new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
            new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(SHORT_AS).setLocalAdministrator(BYTE).build()).build();
    }

    private static ExtendedCommunity opaqueBuild() {
        return new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
            new OpaqueExtendedCommunityBuilder().setValue(BYTE).build()).build();
    }

    private static ExtendedCommunity inet4Build() {
        return new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
            new Inet4SpecificExtendedCommunityBuilder().setGlobalAdministrator(IPV4).setLocalAdministrator(BYTE).build()).build();
    }

    private static ExtendedCommunity asSpecBuild() {
        return new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
            new AsSpecificExtendedCommunityBuilder().setGlobalAdministrator(SHORT_AS).setLocalAdministrator(BYTE).build()).build();
    }
}
