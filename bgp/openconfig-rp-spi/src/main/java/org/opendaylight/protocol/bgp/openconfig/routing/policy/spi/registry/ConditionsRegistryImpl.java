/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionsAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

@SuppressFBWarnings("NM_CONFUSING")
final class ConditionsRegistryImpl {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Conditions>>, ConditionsAugPolicy> conditionsRegistry
            = new HashMap<>();
    private final BgpConditionsRegistry bgpConditionsRegistry = new BgpConditionsRegistry();

    ConditionsRegistryImpl() {
    }

    AbstractRegistration registerConditionPolicy(final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
            final ConditionsAugPolicy conditionPolicy) {
        synchronized (conditionsRegistry) {
            final ConditionsAugPolicy prev = conditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (conditionsRegistry) {
                        conditionsRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    public AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<? extends Augmentation<BgpConditions>> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy conditionPolicy) {
        return bgpConditionsRegistry
                .registerBgpConditionsAugmentationPolicy(conditionPolicyClass, conditionPolicy);
    }

    public <T extends ChildOf<BgpMatchConditions>, N> AbstractRegistration registerBgpConditionsPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsPolicy<T, N> conditionPolicy) {
        return bgpConditionsRegistry
                .registerBgpConditionsPolicy(conditionPolicyClass, conditionPolicy);
    }

    @SuppressWarnings("unchecked")
    boolean matchExportConditions(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final Conditions conditions) {

        if (!bgpConditionsRegistry
                .matchExportConditions(afiSafi, entryInfo, routeEntryExportParameters, attributes, conditions)) {
            return false;
        }

        for (final Augmentation<Conditions> entry : conditions.augmentations().values()) {
            final ConditionsAugPolicy handler = conditionsRegistry.get(entry.implementedInterface());
            if (handler == null) {
                continue;
            }
            if (!handler.matchExportCondition(afiSafi, entryInfo, routeEntryExportParameters,
                handler.getConditionParameter(attributes), entry)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    boolean matchImportConditions(
            final AfiSafiType afiSafi, final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final Conditions conditions) {
        if (!bgpConditionsRegistry
                .matchImportConditions(afiSafi, entryInfo, routeEntryImportParameters, attributes, conditions)) {
            return false;
        }

        if (attributes != null) {
            for (final Augmentation<Conditions> condition : conditions.augmentations().values()) {
                final ConditionsAugPolicy handler = conditionsRegistry.get(condition.implementedInterface());
                if (handler != null) {
                    if (!handler.matchImportCondition(afiSafi, entryInfo, routeEntryImportParameters,
                            handler.getConditionParameter(attributes), condition)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
