/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.RouteTargetIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.constrain._default.route.grouping.RouteTargetConstrainDefaultRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.route.target.constrain._default.route.grouping.RouteTargetConstrainDefaultRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.VpnNonMemberCondition;

/**
 * Returns true if Route Target extended communities attributes are not part of the VPN membership of destiny peer.
 *
 * @author Claudio D. Gasparini
 */
public final class VpnNonMemberHandler
        implements BgpConditionsAugmentationPolicy<VpnNonMemberCondition, List<ExtendedCommunities>> {
    private static final VpnNonMemberHandler INSTANCE = new VpnNonMemberHandler();
    private static final RouteTargetConstrainDefaultRoute DEFAULT = new RouteTargetConstrainDefaultRouteBuilder()
            .build();

    private VpnNonMemberHandler() {

    }

    public static VpnNonMemberHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean matchImportCondition(
            final AfiSafiType afiSafiType,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final List<ExtendedCommunities> attributes,
            final VpnNonMemberCondition conditions) {
        return false;
    }

    @Override
    public boolean matchExportCondition(
            final AfiSafiType afiSafiType,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final List<ExtendedCommunities> attributes,
            final VpnNonMemberCondition conditions) {
        final List<RouteTarget> allowedRouteTarget = routeEntryExportParameters.getMemberships();
        if (allowedRouteTarget.contains(DEFAULT)) {
            return false;
        }
        final List<RouteTarget> toRT = attributes.stream()
                .map(ExtendedCommunities::getExtendedCommunity)
                .filter(Objects::nonNull)
                .filter(this::filterRTExtComm)
                .map(this::extendedCommunityToRouteTarget)
                .collect(Collectors.toList());
        return Collections.disjoint(allowedRouteTarget, toRT);
    }

    private RouteTarget extendedCommunityToRouteTarget(final ExtendedCommunity rt) {
        if (rt instanceof RouteTargetExtendedCommunityCase) {
            return ((RouteTargetExtendedCommunityCase) rt).getRouteTargetExtendedCommunity();
        } else if (rt instanceof As4RouteTargetExtendedCommunityCase) {
            return ((As4RouteTargetExtendedCommunityCase) rt).getAs4RouteTargetExtendedCommunity();
        }
        return ((RouteTargetIpv4Case) rt).getRouteTargetIpv4();
    }

    private boolean filterRTExtComm(final ExtendedCommunity rt) {
        return rt instanceof RouteTargetExtendedCommunityCase || rt instanceof As4RouteTargetExtendedCommunityCase
                || rt instanceof RouteTargetIpv4Case;
    }

    @Override
    public List<ExtendedCommunities> getConditionParameter(final Attributes attributes) {
        final List<ExtendedCommunities> ext = attributes.getExtendedCommunities();
        return ext == null ? List.of() : ext;
    }
}
