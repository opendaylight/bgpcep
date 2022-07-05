/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import com.google.common.collect.ForwardingObject;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionsAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionsAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.BgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

abstract class ForwardingStatementRegistry extends ForwardingObject
        implements StatementRegistryConsumer, StatementRegistryProvider {
    @Override
    public final RouteAttributeContainer applyExportStatement(final RouteEntryBaseAttributes routeEntryInfo,
            final AfiSafiType afiSafi, final BGPRouteEntryExportParameters baseExportParameters,
            final RouteAttributeContainer attributes, final Statement statement) {
        return delegate().applyExportStatement(routeEntryInfo, afiSafi, baseExportParameters, attributes, statement);
    }

    @Override
    public final RouteAttributeContainer applyImportStatement(final RouteEntryBaseAttributes routeEntryInfo,
            final AfiSafiType afiSafi, final BGPRouteEntryImportParameters routeBaseParameters,
            final RouteAttributeContainer attributes, final Statement statement) {
        return delegate().applyImportStatement(routeEntryInfo, afiSafi, routeBaseParameters, attributes, statement);
    }

    @Override
    public final AbstractRegistration registerConditionPolicy(
            final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
                final ConditionsAugPolicy conditionPolicy) {
        return delegate().registerConditionPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public final AbstractRegistration registerActionPolicy(
            final Class<? extends Augmentation<Actions>> actionPolicyClass, final ActionsAugPolicy actionPolicy) {
        return delegate().registerActionPolicy(actionPolicyClass, actionPolicy);
    }

    @Override
    public final <T extends ChildOf<BgpMatchConditions>, N> AbstractRegistration registerBgpConditionsPolicy(
            final Class<T> conditionPolicyClass, final BgpConditionsPolicy<T, N> conditionPolicy) {
        return delegate().registerBgpConditionsPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public final <T extends ChildOf<BgpActions>> AbstractRegistration registerBgpActionPolicy(
            final Class<T> bgpActionPolicyClass, final BgpActionPolicy<T> bgpActionPolicy) {
        return delegate().registerBgpActionPolicy(bgpActionPolicyClass, bgpActionPolicy);
    }

    @Override
    public final <T extends Augmentation<BgpConditions>, N>
            AbstractRegistration registerBgpConditionsAugmentationPolicy(final Class<T> conditionPolicyClass,
                final BgpConditionsAugmentationPolicy<T, N> conditionPolicy) {
        return delegate().registerBgpConditionsAugmentationPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public final <T extends Augmentation<BgpActions>> AbstractRegistration registerBgpActionAugmentationPolicy(
            final Class<T> bgpActionPolicyClass, final BgpActionAugPolicy<T> bgpActionPolicy) {
        return delegate().registerBgpActionAugmentationPolicy(bgpActionPolicyClass, bgpActionPolicy);
    }

    @Override
    protected abstract @NonNull StatementRegistry delegate();
}
