/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractCommunityHandler;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetCommunityOptionType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetCommunity;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.community.SetCommunityMethod;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.community.set.community.method.Inline;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.community.set.community.method.Reference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.CommunitiesBuilder;

/**
 * Prepend / Replace / Remove a set of communities.
 */
public final class SetCommunityHandler extends AbstractCommunityHandler implements BgpActionPolicy<SetCommunity> {
    @NonNullByDefault
    public SetCommunityHandler(final DataBroker dataBroker) {
        super(dataBroker);
    }

    @Override
    public Attributes applyImportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters, final Attributes attributes,
            final SetCommunity bgpActions) {
        return setComm(attributes, bgpActions.getSetCommunityMethod(), bgpActions.getOptions());
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final Attributes attributes,
            final SetCommunity bgpActions) {
        return setComm(attributes, bgpActions.getSetCommunityMethod(), bgpActions.getOptions());
    }

    private Attributes setComm(final Attributes attributes, final SetCommunityMethod setCommunityMethod,
            final BgpSetCommunityOptionType options) {
        return switch (setCommunityMethod) {
            case Inline inline -> inlineSetComm(attributes, inline.nonnullCommunities().stream()
                    .map(ge -> new CommunitiesBuilder()
                        .setAsNumber(ge.getAsNumber())
                        .setSemantics(ge.getSemantics())
                        .build())
                    .toList(), options);
            case Reference reference -> referenceSetComm(attributes, reference.getCommunitySetRef(), options);
            default -> throw new UnsupportedOperationException("Unsupported method " + setCommunityMethod);
        };
    }

    private Attributes referenceSetComm(final Attributes attributes, final String communitySetName,
            final BgpSetCommunityOptionType options) {
        final var setKey = StringUtils.substringBetween(communitySetName, "=\"", "\"");
        return inlineSetComm(attributes, communitySets.getUnchecked(setKey), options);
    }

    private static Attributes inlineSetComm(final Attributes attributes, final List<Communities> actionCommunities,
            final BgpSetCommunityOptionType options) {
        final var newAtt = new AttributesBuilder(attributes);
        if (options.equals(BgpSetCommunityOptionType.REPLACE)) {
            return newAtt.setCommunities(List.copyOf(actionCommunities)).build();
        }

        final var comm = attributes.getCommunities();
        final var actualComm = comm != null ? new ArrayList<>(comm) : new ArrayList<Communities>();
        switch (options) {
            case null -> throw new NullPointerException();
            case ADD -> actualComm.addAll(actionCommunities);
            case REMOVE -> actualComm.removeAll(actionCommunities);
            case REPLACE -> throw new IllegalArgumentException("REPLACE not implemented!");
        }
        return newAtt.setCommunities(actualComm).build();
    }
}
