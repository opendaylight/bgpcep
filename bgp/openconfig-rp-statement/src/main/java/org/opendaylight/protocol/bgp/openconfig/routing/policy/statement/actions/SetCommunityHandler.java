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
    public SetCommunityHandler(final DataBroker dataBroker) {
        super(dataBroker);
    }

    @Override
    public Attributes applyImportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final SetCommunity bgpActions) {
        return setComm(attributes, bgpActions.getSetCommunityMethod(), bgpActions.getOptions());
    }

    @Override
    public Attributes applyExportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final SetCommunity bgpActions) {
        return setComm(attributes, bgpActions.getSetCommunityMethod(), bgpActions.getOptions());
    }

    private Attributes setComm(
            final Attributes attributes,
            final SetCommunityMethod setCommunityMethod,
            final BgpSetCommunityOptionType options) {
        if (setCommunityMethod instanceof Inline) {
            final Inline inline = (Inline) setCommunityMethod;
            final List<Communities> list = inline.getCommunities()
                    .stream().map(ge -> new CommunitiesBuilder().setAsNumber(ge.getAsNumber())
                            .setSemantics(ge.getSemantics()).build()).collect(Collectors.toList());
            return inlineSetComm(attributes, list, options);
        }
        return referenceSetComm(attributes, ((Reference) setCommunityMethod).getCommunitySetRef(), options);
    }

    private Attributes referenceSetComm(
            final Attributes attributes,
            final String communitySetName,
            final BgpSetCommunityOptionType options) {
        final String setKey = StringUtils.substringBetween(communitySetName, "=\"", "\"");
        return inlineSetComm(attributes, this.communitySets.getUnchecked(setKey), options);
    }

    private static Attributes inlineSetComm(
            final Attributes attributes,
            final List<Communities> actionCommunities,
            final BgpSetCommunityOptionType options) {

        final AttributesBuilder newAtt = new AttributesBuilder(attributes);

        if (options.equals(BgpSetCommunityOptionType.REPLACE)) {
            return newAtt.setCommunities(actionCommunities).build();
        }

        final List<Communities> actualComm;
        if (attributes.getCommunities() != null) {
            actualComm = new ArrayList<>(attributes.getCommunities());
        } else {
            actualComm = new ArrayList<>();
        }

        switch (options) {
            case ADD:
                actualComm.addAll(actionCommunities);
                break;
            case REMOVE:
                actualComm.removeAll(actionCommunities);
                break;
            default:
                throw new IllegalArgumentException("Option Type not Recognized!");
        }

        return newAtt.setCommunities(actualComm).build();
    }
}
