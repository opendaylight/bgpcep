/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetResolver;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.DefinedSetsIndex;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetCommunityOptionType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetExtCommunity;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.set.ext.community.method.Inline;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.set.ext.community.method.Reference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;

/**
 * Prepend External Community.
 */
public final class SetExtCommunityHandler implements BgpActionPolicy<SetExtCommunity> {
    private final @NonNull DefinedSetsIndex resolver;

    public SetExtCommunityHandler(final DefinedSetResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

    @Override
    public Attributes applyImportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters, final Attributes attributes,
            final SetExtCommunity bgpActions) {
        return setExtComm(attributes, bgpActions);
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final Attributes attributes,
            final SetExtCommunity bgpActions) {
        return setExtComm(attributes, bgpActions);
    }

    private Attributes setExtComm(final Attributes attributes, final SetExtCommunity bgpActions) {
        final var method = bgpActions.getSetExtCommunityMethod();
        return switch (method) {
            case Inline inline -> inlineSetExtComm(attributes, inline.nonnullExtCommunityMember().stream()
                .map(ge -> new ExtendedCommunitiesBuilder()
                    .setExtendedCommunity(ge.getExtendedCommunity())
                    .setTransitive(ge.getTransitive())
                    .build())
                .toList(), bgpActions.getOptions());
            case Reference reference ->
                inlineSetExtComm(attributes, resolver.lookupExtCommunitySet(reference.getExtCommunitySetRef()),
                    bgpActions.getOptions());
            default -> throw new UnsupportedOperationException("Unsupported " + method.implementedCase().getName());
        };
    }

    private static Attributes inlineSetExtComm(final Attributes attributes,
            final List<ExtendedCommunities> actionExtCommunities, final BgpSetCommunityOptionType options) {
        return new AttributesBuilder(attributes)
            .setExtendedCommunities(switch (options) {
                case ADD -> {
                    final var extComm = attributes.getExtendedCommunities();
                    yield extComm == null || extComm.isEmpty() ? actionExtCommunities
                        : Stream.concat(extComm.stream(), actionExtCommunities.stream()).toList();
                }
                case REMOVE -> {
                    final var extComm = attributes.getExtendedCommunities();
                    if (extComm == null || extComm.isEmpty()) {
                        yield extComm;
                    }
                    final var actualComm = new ArrayList<>(extComm);
                    actualComm.removeAll(actionExtCommunities);
                    yield List.copyOf(actualComm);
                }
                case REPLACE -> actionExtCommunities;
            })
            .build();
    }
}
