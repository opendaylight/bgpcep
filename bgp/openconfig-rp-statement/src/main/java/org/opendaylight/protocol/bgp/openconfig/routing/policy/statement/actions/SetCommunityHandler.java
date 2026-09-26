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
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractCommunityHandler;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetCommunityOptionType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetCommunity;
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
        return setComm(attributes, bgpActions);
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final Attributes attributes,
            final SetCommunity bgpActions) {
        return setComm(attributes, bgpActions);
    }

    private Attributes setComm(final Attributes attributes, final SetCommunity bgpActions) {
        final var setCommunityMethod = bgpActions.getSetCommunityMethod();
        return switch (setCommunityMethod) {
            case Inline inline -> inlineSetComm(attributes, inline.nonnullCommunities().stream()
                .map(ge -> new CommunitiesBuilder()
                    .setAsNumber(ge.getAsNumber())
                    .setSemantics(ge.getSemantics())
                    .build())
                .toList(), bgpActions.getOptions());
            case Reference reference ->
                inlineSetComm(attributes, lookupCommunitySet(reference.getCommunitySetRef()), bgpActions.getOptions());
            default -> throw new UnsupportedOperationException(
                "Unsupported method " + setCommunityMethod.implementedCase().getName());
        };
    }

    private static Attributes inlineSetComm(final Attributes attributes, final List<Communities> actionCommunities,
            final BgpSetCommunityOptionType options) {
        return new AttributesBuilder(attributes)
            .setCommunities(switch (options) {
                case ADD -> {
                    final var comm = attributes.getCommunities();
                    yield comm == null || comm.isEmpty() ? List.copyOf(actionCommunities)
                        : Stream.concat(comm.stream(), actionCommunities.stream()).toList();
                }
                case REMOVE -> {
                    final var comm = attributes.getCommunities();
                    if (comm == null || comm.isEmpty()) {
                        yield List.of();
                    }
                    final var ret = new ArrayList<>(comm);
                    ret.removeAll(actionCommunities);
                    yield List.copyOf(ret);
                }
                case REPLACE -> List.copyOf(actionCommunities);
            })
            .build();
    }
}
