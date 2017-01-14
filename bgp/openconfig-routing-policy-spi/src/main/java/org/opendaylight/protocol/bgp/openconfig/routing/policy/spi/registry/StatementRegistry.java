/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.ActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.ConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.IGPAugActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.IGPConditionPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class StatementRegistry implements StatementRegistryConsumer, StatementRegistryProvider {
    private final ConditionsRegistryImpl conditionsRegistry;
    private final ActionsRegistryImpl actionsRegistry;

    public StatementRegistry() {
        this.conditionsRegistry = new ConditionsRegistryImpl();
        this.actionsRegistry = new ActionsRegistryImpl();
    }

    @Override
    public RouteAttributeContainer applyExportStatement(final OpenconfigPolicyConsumer policyConsumer, final long localAs,
        final Ipv4Address originatorId, final ClusterIdentifier clusterId, final PathArgument routeKey,
        final PeerId fromPeerId, final PeerRole fromPeerRole, final PeerId toPeer, final PeerRole toPeerRole,
        final RouteAttributeContainer attributes, final Statement statement) {
        final Actions actions = statement.getActions();
        final ContainerNode att = attributes.getAttributes().get();
        if (!this.conditionsRegistry.matchExportConditions(policyConsumer, routeKey, fromPeerId, fromPeerRole, toPeer,
            toPeerRole, att, statement.getConditions())) {
            return attributes;
        }
        return new RouteAttributeContainer(this.actionsRegistry.applyExportAction(localAs, originatorId,
            clusterId, routeKey, fromPeerId, fromPeerRole, toPeer, toPeerRole, att,
            actions), true);
    }

    @Override
    public RouteAttributeContainer applyImportStatement(final OpenconfigPolicyConsumer policyConsumer,
        final long localAs, final Ipv4Address originatorId, final ClusterIdentifier clusterId,
        final PathArgument routeKey, final PeerId fromPeerId, final PeerRole fromPeerRole,
        final RouteAttributeContainer attributes, final Statement statement) {
        final ContainerNode att = attributes.getAttributes().get();
        if (!this.conditionsRegistry.matchImportConditions(policyConsumer, routeKey, fromPeerId, fromPeerRole,
            att, statement.getConditions())) {
            return attributes;
        }
        return new RouteAttributeContainer(this.actionsRegistry
            .applyImportAction(localAs, originatorId, clusterId, routeKey, fromPeerId, fromPeerRole, att,
                statement.getActions()), true);
    }

    @Override
    public AbstractRegistration registerConditionPolicy(final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
        final ConditionPolicy conditionPolicy) {
        return this.conditionsRegistry.registerConditionPolicy(conditionPolicyClass, conditionPolicy);
    }

    @Override
    public AbstractRegistration registerIGPConditionPolicy(final Class<? extends Augmentation<IgpConditions>> igpConditionClass,
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
        final IGPAugActionPolicy igpActionPolicy) {
        return this.actionsRegistry.registerIGPActionPolicy(igpActionPolicyClass, igpActionPolicy);
    }
}
