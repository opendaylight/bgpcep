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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.IgpActionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.GenericConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.IGPConditionsPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.BgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
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
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final RouteAttributeContainer attributes,
            final Statement statement) {
        if (!this.conditionsRegistry.matchExportConditions(
                routeEntryInfo,
                routeEntryExportParameters,
                attributes.getAttributes(),
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
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final RouteAttributeContainer attributes,
            final Statement statement) {
        final Attributes att = attributes.getAttributes();
        if (att == null || !this.conditionsRegistry.matchImportConditions(
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
    public AbstractRegistration registerGenericConditionPolicy(
            final GenericConditionsPolicy conditionPolicy) {
        return this.conditionsRegistry.registerGenericConditionPolicy(conditionPolicy);
    }

    @Override
    public AbstractRegistration registerConditionPolicy(
            final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
            final ConditionsPolicy conditionPolicy) {
        return this.conditionsRegistry.registerConditionPolicy(conditionPolicyClass, conditionPolicy);
    }


    @Override
    public AbstractRegistration registerIGPConditionPolicy(
            final Class<? extends Augmentation<IgpConditions>> igpConditionClass,
            final IGPConditionsPolicy igpConditionPolicy) {
        return this.conditionsRegistry.registerIGPConditionPolicy(igpConditionClass, igpConditionPolicy);
    }

    @Override
    public AbstractRegistration registerActionPolicy(
            final Class<? extends Augmentation<Actions>> actionPolicyClass,
            final ActionsPolicy actionPolicy) {
        return this.actionsRegistry.registerActionPolicy(actionPolicyClass, actionPolicy);
    }

    @Override
    public AbstractRegistration registerIGPActionPolicy(
            final Class<? extends Augmentation<IgpActions>> igpActionPolicyClass,
            final IgpActionsPolicy igpActionPolicy) {
        return this.actionsRegistry.registerIGPActionPolicy(igpActionPolicyClass, igpActionPolicy);
    }

    @Override
    public <T extends Augmentation<BgpConditions>> AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy<T> conditionPolicy) {
        return this.conditionsRegistry.registerBgpConditionsAugmentationPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public AbstractRegistration registerBgpConditionsPolicy(final BgpConditionsPolicy conditionPolicy) {
        return this.conditionsRegistry.registerBgpConditionsPolicy(conditionPolicy);
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
