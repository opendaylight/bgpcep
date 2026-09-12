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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.AbstractExtCommunityHandler;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetCommunityOptionType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetExtCommunity;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.SetExtCommunityMethod;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.set.ext.community.method.Inline;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.set.ext.community.method.Reference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;

/**
 * Prepend External Community.
 */
public final class SetExtCommunityHandler extends AbstractExtCommunityHandler
        implements BgpActionPolicy<SetExtCommunity> {
    @NonNullByDefault
    public SetExtCommunityHandler(final DataBroker dataBroker) {
        super(dataBroker);
    }

    @Override
    public Attributes applyImportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters, final Attributes attributes,
            final SetExtCommunity bgpActions) {
        return setExtComm(attributes, bgpActions.getSetExtCommunityMethod(), bgpActions.getOptions());
    }

    @Override
    public Attributes applyExportAction(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters, final Attributes attributes,
            final SetExtCommunity bgpActions) {
        return setExtComm(attributes, bgpActions.getSetExtCommunityMethod(), bgpActions.getOptions());
    }

    private Attributes setExtComm(final Attributes attributes, final SetExtCommunityMethod setExtCommunityMethod,
            final BgpSetCommunityOptionType options) {
        return switch (setExtCommunityMethod) {
            case Inline inline -> inlineSetExtComm(attributes, inline.nonnullExtCommunityMember().stream()
                .map(ge -> new ExtendedCommunitiesBuilder()
                    .setExtendedCommunity(ge.getExtendedCommunity())
                    .setTransitive(ge.getTransitive())
                    .build())
                .collect(Collectors.toList()), options);
            case Reference reference -> referenceSetExtComm(attributes, reference.getExtCommunitySetRef(), options);
            default -> throw new IllegalArgumentException("Unhandled " + setExtCommunityMethod);
        };
    }

    private static Attributes inlineSetExtComm(final Attributes attributes,
            final List<ExtendedCommunities> actionExtCommunities, final BgpSetCommunityOptionType options) {
        final var newAtt = new AttributesBuilder(attributes);
        if (options.equals(BgpSetCommunityOptionType.REPLACE)) {
            return newAtt.setExtendedCommunities(actionExtCommunities).build();
        }

        final List<ExtendedCommunities> actualComm;
        if (attributes.getCommunities() != null) {
            actualComm = new ArrayList<>(attributes.getExtendedCommunities());
        } else {
            actualComm = new ArrayList<>();
        }

        switch (options) {
            case null -> throw new NullPointerException();
            case ADD -> actualComm.addAll(actionExtCommunities);
            case REMOVE -> actualComm.removeAll(actionExtCommunities);
            case REPLACE-> throw new IllegalArgumentException("REPLACE not supported");
        }
        return newAtt.setExtendedCommunities(actualComm).build();
    }

    private Attributes referenceSetExtComm(final Attributes attributes, final String extCommunitySetName,
            final BgpSetCommunityOptionType options) {
        return inlineSetExtComm(attributes,
            // TODO: can we ditch commons-lang3 here?
            extCommunitySets.getUnchecked(StringUtils.substringBetween(extCommunitySetName, "=\"", "\"")), options);
    }

}
