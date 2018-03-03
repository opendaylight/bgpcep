/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.BgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

public interface BgpStatementRegistryProvider {
    /**
     * Register Bgp Condition Policy Augmentation handler.
     *
     * @param conditionPolicyClass Conditions Augmentation Class
     * @param conditionPolicy      Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    <T extends ChildOf<BgpMatchConditions>, N> AbstractRegistration registerBgpConditionsPolicy(
            @Nonnull Class<T> conditionPolicyClass,
            @Nonnull BgpConditionsPolicy<T, N> conditionPolicy);

    /**
     * Register BGP Action Policy Augmentation handler.
     *
     * @param bgpActionPolicyClass BGP Actions Augmentation Class
     * @param bgpActionPolicy      BGP Actions policy handler
     * @return registration ticket
     */
    @Nonnull
    <T extends ChildOf<BgpActions>> AbstractRegistration registerBgpActionPolicy(
            @Nonnull Class<T> bgpActionPolicyClass,
            @Nonnull BgpActionPolicy<T> bgpActionPolicy);

    /**
     * Register Bgp Condition Policy Augmentation handler.
     *
     * @param conditionPolicyClass Conditions Augmentation Class
     * @param conditionPolicy      Condition policy handler
     * @return registration ticket
     */
    @Nonnull
    <T extends Augmentation<BgpConditions>, N> AbstractRegistration registerBgpConditionsAugmentationPolicy(
            @Nonnull Class<T> conditionPolicyClass,
            @Nonnull BgpConditionsAugmentationPolicy<T, N> conditionPolicy);

    /**
     * Register BGP Action Policy Augmentation handler.
     *
     * @param bgpActionPolicyClass BGP Actions Augmentation Class
     * @param bgpActionPolicy      BGP Actions policy handler
     * @return registration ticket
     */
    @Nonnull
    <T extends Augmentation<BgpActions>> AbstractRegistration registerBgpActionAugmentationPolicy(
            @Nonnull Class<T> bgpActionPolicyClass,
            @Nonnull BgpActionAugPolicy<T> bgpActionPolicy);
}
