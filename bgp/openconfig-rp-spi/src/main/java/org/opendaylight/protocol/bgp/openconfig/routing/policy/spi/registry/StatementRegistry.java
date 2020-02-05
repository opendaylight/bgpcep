/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import static org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.RouteAttributeContainer.routeAttributeContainerTrue;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionsAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionsAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.BgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public final class StatementRegistry implements StatementRegistryConsumer, StatementRegistryProvider {
    private final ConditionsRegistryImpl conditionsRegistry;
    private final ActionsRegistryImpl actionsRegistry;

    public StatementRegistry() {
        this.conditionsRegistry = new ConditionsRegistryImpl();
        this.actionsRegistry = new ActionsRegistryImpl();
    }

    @Override
    public RouteAttributeContainer applyExportStatement(
            final RouteEntryBaseAttributes routeEntryInfo,
            final Class<? extends AfiSafiType> afiSafi,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final RouteAttributeContainer attributes,
            final Statement statement) {
        final Attributes att = attributes.getAttributes();
        if (att == null || !this.conditionsRegistry.matchExportConditions(
                afiSafi,
                routeEntryInfo,
                routeEntryExportParameters,
                att,
                statement.getConditions())) {
            return attributes;
        }
        return routeAttributeContainerTrue(this.actionsRegistry.applyExportAction(
                routeEntryInfo,
                routeEntryExportParameters,
                attributes.getAttributes(),
                statement.getActions()));
    }

    @Override
    public RouteAttributeContainer applyImportStatement(
            final RouteEntryBaseAttributes routeEntryInfo,
            final Class<? extends AfiSafiType> afiSafi,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final RouteAttributeContainer attributes,
            final Statement statement) {
        final Attributes att = attributes.getAttributes();
        if (att == null || !this.conditionsRegistry.matchImportConditions(
                afiSafi,
                routeEntryInfo,
                routeEntryImportParameters,
                attributes.getAttributes(),
                statement.getConditions())) {
            return attributes;
        }
        return routeAttributeContainerTrue(this.actionsRegistry.applyImportAction(
                routeEntryInfo,
                routeEntryImportParameters,
                att,
                statement.getActions()));
    }

    @Override
    public AbstractRegistration registerConditionPolicy(
            final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
            final ConditionsAugPolicy conditionPolicy) {
        return this.conditionsRegistry.registerConditionPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public <T extends ChildOf<BgpMatchConditions>, N> AbstractRegistration registerBgpConditionsPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsPolicy<T, N> conditionPolicy) {
        return this.conditionsRegistry.registerBgpConditionsPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public AbstractRegistration registerActionPolicy(
            final Class<? extends Augmentation<Actions>> actionPolicyClass,
            final ActionsAugPolicy actionPolicy) {
        return this.actionsRegistry.registerActionPolicy(actionPolicyClass, actionPolicy);
    }

    @Override
    public <T extends Augmentation<BgpConditions>, N> AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy<T, N> conditionPolicy) {
        return this.conditionsRegistry.registerBgpConditionsAugmentationPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public <T extends Augmentation<BgpActions>> AbstractRegistration registerBgpActionAugmentationPolicy(
            final Class<T> bgpActionPolicyClass,
            final BgpActionAugPolicy<T> bgpActionPolicy) {
        return this.actionsRegistry.registerBgpActionAugmentationPolicy(bgpActionPolicyClass, bgpActionPolicy);
    }

    @Override
    public <T extends ChildOf<BgpActions>> AbstractRegistration registerBgpActionPolicy(
            final Class<T> bgpActionPolicyClass,
            final BgpActionPolicy<T> bgpActionPolicy) {
        return this.actionsRegistry.registerBgpActionPolicy(bgpActionPolicyClass, bgpActionPolicy);
    }
}
