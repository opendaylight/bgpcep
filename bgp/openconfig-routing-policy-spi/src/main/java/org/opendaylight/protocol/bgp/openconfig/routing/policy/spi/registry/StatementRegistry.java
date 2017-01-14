/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.PolicyRIBBaseParameters;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.IgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.IGPConditionPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteBaseParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class StatementRegistry implements StatementRegistryConsumer, StatementRegistryProvider {
    private final ConditionsRegistryImpl conditionsRegistry;
    private final ActionsRegistryImpl actionsRegistry;

    public StatementRegistry() {
        this.conditionsRegistry = new ConditionsRegistryImpl();
        this.actionsRegistry = new ActionsRegistryImpl();
    }

    @Override
    public RouteAttributeContainer applyExportStatement(
        final OpenconfigPolicyConsumer policyConsumer,
        final PolicyRIBBaseParameters basePolicyParameters,
        final BGPRouteBaseExportParameters baseExportParameters,
        final RouteAttributeContainer attributes,
        final Statement statement) {
        final Actions actions = statement.getActions();
        if (!this.conditionsRegistry.matchExportConditions(policyConsumer, basePolicyParameters, baseExportParameters,
            attributes.getAttributes(), statement.getConditions())) {
            return attributes;
        }
        return new RouteAttributeContainer(this.actionsRegistry
            .applyExportAction(basePolicyParameters, baseExportParameters,
            attributes.getAttributes(), actions),
            true);
    }

    @Override
    public RouteAttributeContainer applyImportStatement(
        final OpenconfigPolicyConsumer policyConsumer,
        final PolicyRIBBaseParameters basePolicyParameters,
        final BGPRouteBaseParameters routeBaseParameters,
        final RouteAttributeContainer attributes,
        final Statement statement) {
        final ContainerNode att = attributes.getAttributes();
        if (att == null || !this.conditionsRegistry.matchImportConditions(policyConsumer, basePolicyParameters,
            routeBaseParameters, attributes.getAttributes(), statement.getConditions())) {
            return attributes;
        }
        return new RouteAttributeContainer(this.actionsRegistry
            .applyImportAction(basePolicyParameters, routeBaseParameters, att,
                statement.getActions()), true);
    }

    @Override
    public AbstractRegistration registerConditionPolicy(
        final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
        final ConditionPolicy conditionPolicy) {
        return this.conditionsRegistry.registerConditionPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public AbstractRegistration registerIGPConditionPolicy(
        final Class<? extends Augmentation<IgpConditions>> igpConditionClass,
        final IGPConditionPolicy igpConditionPolicy) {
        return this.conditionsRegistry.registerIGPConditionPolicy(igpConditionClass, igpConditionPolicy);
    }

    @Override
    public AbstractRegistration registerActionPolicy(final Class<? extends Augmentation<Actions>> actionPolicyClass,
        final ActionPolicy actionPolicy) {
        return this.actionsRegistry.registerActionPolicy(actionPolicyClass, actionPolicy);
    }

    @Override
    public AbstractRegistration registerIGPActionPolicy(
        final Class<? extends Augmentation<IgpActions>> igpActionPolicyClass,
        final IgpActionPolicy igpActionPolicy) {
        return this.actionsRegistry.registerIGPActionPolicy(igpActionPolicyClass, igpActionPolicy);
    }
}
