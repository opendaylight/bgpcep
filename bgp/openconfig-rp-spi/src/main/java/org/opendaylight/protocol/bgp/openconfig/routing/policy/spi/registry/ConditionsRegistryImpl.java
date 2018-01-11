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
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BGPConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.GenericConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.IGPConditionPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Conditions1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

@SuppressFBWarnings("NM_CONFUSING")
final class ConditionsRegistryImpl {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Conditions>>, ConditionPolicy> conditionsRegistry = new HashMap<>();
    private final Map<Class<? extends Augmentation<BgpConditions>>,
            BGPConditionPolicy> bgpConditionsRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy
            .rev151009.igp.conditions.IgpConditions>>, IGPConditionPolicy> igpConditionsRegistry = new HashMap<>();
    @GuardedBy("this")
    private GenericConditionPolicy genericConditionHandler;

    AbstractRegistration registerBGPConditionPolicy(
            final Class<? extends Augmentation<BgpConditions>> conditionPolicyClass,
            final BGPConditionPolicy conditionPolicy) {
        synchronized (this.bgpConditionsRegistry) {
            final BGPConditionPolicy prev
                    = this.bgpConditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ConditionsRegistryImpl.this.bgpConditionsRegistry) {
                        ConditionsRegistryImpl.this.bgpConditionsRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    AbstractRegistration registerGenericConditionPolicy(final GenericConditionPolicy conditionPolicy) {
        synchronized (this) {
            this.genericConditionHandler = conditionPolicy;
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ConditionsRegistryImpl.this) {
                        ConditionsRegistryImpl.this.genericConditionHandler = null;
                    }
                }
            };
        }
    }

    AbstractRegistration registerConditionPolicy(final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
            final ConditionPolicy conditionPolicy) {
        synchronized (this.conditionsRegistry) {
            final ConditionPolicy prev = this.conditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ConditionsRegistryImpl.this.conditionsRegistry) {
                        ConditionsRegistryImpl.this.conditionsRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    AbstractRegistration registerIGPConditionPolicy(
            final Class<? extends Augmentation<IgpConditions>> igpConditionClass,
            final IGPConditionPolicy igpConditionPolicy
    ) {
        synchronized (this.igpConditionsRegistry) {
            final IGPConditionPolicy prev = this.igpConditionsRegistry
                    .putIfAbsent(igpConditionClass, igpConditionPolicy);
            Preconditions.checkState(prev == null,
                    "IGP Condition Policy %s already registered %s", igpConditionPolicy, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ConditionsRegistryImpl.this.igpConditionsRegistry) {
                        ConditionsRegistryImpl.this.igpConditionsRegistry.remove(igpConditionClass);
                    }
                }
            };
        }
    }

    boolean matchExportConditions(
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final ContainerNode attributes,
            final Conditions conditions) {
        synchronized (this) {
            if (this.genericConditionHandler != null) {
                if (!this.genericConditionHandler
                        .matchExportCondition(entryInfo, routeEntryExportParameters, attributes, conditions)) {
                    return false;
                }
            }
        }

        final IgpConditions igpConditions = conditions.getIgpConditions();
        if (igpConditions != null) {
            final Map<Class<? extends Augmentation<?>>, Augmentation<?>> igpAug = BindingReflections
                    .getAugmentations(igpConditions);
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : igpAug.entrySet()) {
                final IGPConditionPolicy handler = this.igpConditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchExportCondition(entryInfo, routeEntryExportParameters, attributes,
                        (Augmentation<IgpConditions>) entry.getValue())) {
                    return false;
                }
            }
        }

        final Conditions1 bgpConditionsAug = conditions.getAugmentation(Conditions1.class);
        if (bgpConditionsAug != null) {
            final BgpConditions bgpConditions = bgpConditionsAug.getBgpConditions();
            //TODO cada caso
            //bgpConditions.get**

            final Map<Class<? extends Augmentation<?>>, Augmentation<?>> bgpAug = BindingReflections
                    .getAugmentations(bgpConditions);
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : bgpAug.entrySet()) {
                final BGPConditionPolicy handler = this.bgpConditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchExportCondition(entryInfo, routeEntryExportParameters, attributes,
                        (Augmentation<BgpConditions>) entry.getValue())) {
                    return false;
                }
            }
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(conditions);

        if (conditionsAug != null) {
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
                final ConditionPolicy handler = this.conditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchExportCondition(entryInfo, routeEntryExportParameters, attributes,
                        (Augmentation<Conditions>) entry.getValue())) {
                    return false;
                }
            }
        }

        return true;
    }

    boolean matchImportConditions(
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final ContainerNode attributes,
            final Conditions conditions) {

        synchronized (this) {
            if (this.genericConditionHandler != null) {
                if (!this.genericConditionHandler
                        .matchImportCondition(entryInfo, routeEntryImportParameters, attributes, conditions)) {
                    return false;
                }
            }
        }

        final IgpConditions igpConditions = conditions.getIgpConditions();
        if (igpConditions != null) {
            final Map<Class<? extends Augmentation<?>>, Augmentation<?>> igpAug = BindingReflections
                    .getAugmentations(igpConditions);

            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : igpAug.entrySet()) {
                final IGPConditionPolicy handler = this.igpConditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchImportCondition(entryInfo, routeEntryImportParameters, attributes,
                        (Augmentation<IgpConditions>) entry.getValue())) {
                    return false;
                }
            }
        }

        final Conditions1 bgpConditionsAug = conditions.getAugmentation(Conditions1.class);
        if (bgpConditionsAug != null) {
            final BgpConditions bgpConditions = bgpConditionsAug.getBgpConditions();
            //TODO cada caso
            //bgpConditions.get**

            final Map<Class<? extends Augmentation<?>>, Augmentation<?>> bgpAug = BindingReflections
                    .getAugmentations(bgpConditions);
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : bgpAug.entrySet()) {
                final BGPConditionPolicy handler = this.bgpConditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchImportCondition(entryInfo, routeEntryImportParameters, attributes,
                        (Augmentation<BgpConditions>) entry.getValue())) {
                    return false;
                }
            }
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(conditions);

        if (conditionsAug != null && attributes != null) {
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
                final ConditionPolicy handler = this.conditionsRegistry.get(entry.getKey());
                if (handler != null) {
                    final Augmentation<Conditions> conditionConfig = (Augmentation<Conditions>) entry.getValue();
                    if (!handler.matchImportCondition(entryInfo, routeEntryImportParameters, attributes,
                            conditionConfig)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
