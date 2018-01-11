/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionsAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionsAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.GenericConditionsPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;

/**
 * Interface for registering Statement Policies handlers.
 */
public interface StatementRegistryProvider extends
        IGPStatementRegistryProvider, BgpStatementRegistryProvider {
    /**
     * Register Condition Policy Augmentation handler.
     *
     * @param genericConditionPolicy Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerGenericConditionPolicy(
            @Nonnull GenericConditionsPolicy genericConditionPolicy);

    /**
     * Register Condition Policy Augmentation handler.
     *
     * @param conditionPolicyClass Conditions Augmentation Class
     * @param conditionPolicy      Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerConditionPolicy(
            @Nonnull Class<? extends Augmentation<Conditions>> conditionPolicyClass,
            @Nonnull ConditionsAugPolicy conditionPolicy);

    /**
     * Register Action Policy Augmentation handler.
     *
     * @param actionPolicyClass IGP Actions Augmentation Class
     * @param actionPolicy      IGP Actions policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerActionPolicy(
            @Nonnull Class<? extends Augmentation<Actions>> actionPolicyClass,
            @Nonnull ActionsAugPolicy actionPolicy);
}
