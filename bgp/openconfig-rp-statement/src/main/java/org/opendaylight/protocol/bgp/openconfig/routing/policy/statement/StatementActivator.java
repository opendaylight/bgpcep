/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.AbstractBGPStatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.AsPathPrepend;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.LocalAsPathPrependHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.NonTransitiveAttributesFilterHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.SetClusterIdPrependHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.SetCommunityHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.SetExtCommunityHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.SetOriginatorIdPrependHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchAfiSafiNotInHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchAsPathSetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchBgpNeighborSetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchClusterIdSetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchCommunitySetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchExtCommunitySetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchOriginatorIdSetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchRoleSetHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.VpnNonMemberHandler;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchAsPathSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetAsPathPrepend;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetCommunity;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetExtCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.LocalAsPathPrepend;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchAfiSafiNotInCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchBgpNeighborCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchClusterIdSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchOriginatorIdSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.MatchRoleSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.NonTransitiveAttributesFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.SetClusterIdPrepend;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.SetOriginatorIdPrepend;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev200120.VpnNonMemberCondition;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Singleton
@Component(
    immediate = true,
    service = StatementProviderActivator.class,
    property = "type=org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.StatementActivator")
public final class StatementActivator extends AbstractBGPStatementProviderActivator {
    private final @NonNull DefinedSetsIndex definedSets;

    @Inject
    @Activate
    public StatementActivator(@Reference final DefinedSetsIndex definedSets) {
        this.definedSets = requireNonNull(definedSets);
    }

    @Override
    protected synchronized List<Registration> startImpl(final StatementRegistryProvider context) {
        return List.of(
            // Register actions
            context.registerBgpActionPolicy(SetAsPathPrepend.class, AsPathPrepend.getInstance()),
            context.registerBgpActionAugmentationPolicy(LocalAsPathPrepend.class,
                LocalAsPathPrependHandler.getInstance()),
            context.registerBgpActionPolicy(SetCommunity.class, new SetCommunityHandler(definedSets)),
            context.registerBgpActionPolicy(SetExtCommunity.class, new SetExtCommunityHandler(definedSets)),
            context.registerBgpActionAugmentationPolicy(SetOriginatorIdPrepend.class,
                SetOriginatorIdPrependHandler.getInstance()),
            context.registerBgpActionAugmentationPolicy(NonTransitiveAttributesFilter.class,
                NonTransitiveAttributesFilterHandler.getInstance()),
            context.registerBgpActionAugmentationPolicy(SetClusterIdPrepend.class,
                SetClusterIdPrependHandler.getInstance()),

            // Register conditions
            context.registerBgpConditionsAugmentationPolicy(MatchRoleSetCondition.class,
                new MatchRoleSetHandler(definedSets)),
            context.registerBgpConditionsAugmentationPolicy(MatchOriginatorIdSetCondition.class,
                new MatchOriginatorIdSetHandler(definedSets)),
            context.registerBgpConditionsAugmentationPolicy(MatchClusterIdSetCondition.class,
                new MatchClusterIdSetHandler(definedSets)),
            context.registerBgpConditionsPolicy(MatchAsPathSet.class, new MatchAsPathSetHandler(definedSets)),
            context.registerBgpConditionsPolicy(MatchExtCommunitySet.class, new MatchExtCommunitySetHandler(definedSets)),
            context.registerBgpConditionsPolicy(MatchCommunitySet.class, new MatchCommunitySetHandler(definedSets)),
            context.registerBgpConditionsAugmentationPolicy(MatchBgpNeighborCondition.class,
                new MatchBgpNeighborSetHandler(definedSets)),
            context.registerBgpConditionsAugmentationPolicy(MatchAfiSafiNotInCondition.class,
                MatchAfiSafiNotInHandler.getInstance()),
            context.registerBgpConditionsAugmentationPolicy(VpnNonMemberCondition.class,
                VpnNonMemberHandler.getInstance()));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("definedSets", definedSets).toString();
    }
}
