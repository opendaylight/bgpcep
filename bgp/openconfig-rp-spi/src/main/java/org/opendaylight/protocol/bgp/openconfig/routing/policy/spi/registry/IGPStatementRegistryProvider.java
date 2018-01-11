/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.IgpActionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.IGPConditionsPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public interface IGPStatementRegistryProvider {
    /**
     * Register IGP Condition Policy Augmentation handler.
     *
     * @param igpConditionClass  IGP Conditions Augmentation Class
     * @param igpConditionPolicy IGP Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerIGPConditionPolicy(
            @Nonnull Class<? extends Augmentation<IgpConditions>> igpConditionClass,
            @Nonnull IGPConditionsPolicy igpConditionPolicy);

    /**
     * Register IGP Action Policy Augmentation handler.
     *
     * @param igpActionPolicyClass IGP Actions Augmentation Class
     * @param igpActionPolicy      IGP Actions policy handler
     * @return registration ticket
     */
    @Nonnull
    AbstractRegistration registerIGPActionPolicy(
            @Nonnull Class<? extends Augmentation<IgpActions>> igpActionPolicyClass,
            @Nonnull IgpActionsPolicy igpActionPolicy);
}
