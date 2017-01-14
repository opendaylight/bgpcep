/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.IgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.IGPConditionPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy
    .policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.http.
    openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;

/**
 * Interface for registering Statement Policies handlers.
 */
public interface StatementRegistryProvider {
    /**
     * Register Condition Policy Augmentation handler
     *
     * @param conditionPolicyClass Conditions Augmentation Class
     * @param conditionPolicy      Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerConditionPolicy(Class<? extends Augmentation<Conditions>> conditionPolicyClass,
        @Nonnull ConditionPolicy conditionPolicy);

    /**
     * Register IGP Condition Policy Augmentation handler
     *
     * @param igpConditionClass  IGP Conditions Augmentation Class
     * @param igpConditionPolicy IGP Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerIGPConditionPolicy(Class<? extends Augmentation<IgpConditions>> igpConditionClass,
        @Nonnull IGPConditionPolicy igpConditionPolicy);

    /**
     * Register Action Policy Augmentation handler
     *
     * @param actionPolicyClass IGP Actions Augmentation Class
     * @param actionPolicy      IGP Actions policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerActionPolicy(Class<? extends Augmentation<Actions>> actionPolicyClass,
        @Nonnull ActionPolicy actionPolicy);

    /**
     * Register IGP Action Policy Augmentation handler
     *
     * @param igpActionPolicyClass IGP Actions Augmentation Class
     * @param igpActionPolicy      IGP Actions policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerIGPActionPolicy(Class<? extends Augmentation<IgpActions>> igpActionPolicyClass,
        @Nonnull IgpActionPolicy igpActionPolicy);
}
