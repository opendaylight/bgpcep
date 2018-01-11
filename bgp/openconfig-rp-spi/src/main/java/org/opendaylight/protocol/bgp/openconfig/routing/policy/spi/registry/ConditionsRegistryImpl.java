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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.ConditionsAugPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

@SuppressFBWarnings("NM_CONFUSING")
final class ConditionsRegistryImpl {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Conditions>>, ConditionsAugPolicy> conditionsRegistry
            = new HashMap<>();
    private final GenericConditionPolicyHandler genericConditionHandler;
    private BgpConditionsRegistry bgpConditionsRegistry = new BgpConditionsRegistry();

    ConditionsRegistryImpl(final DataBroker databroker) {
        this.genericConditionHandler = new GenericConditionPolicyHandler(databroker);
    }

    AbstractRegistration registerConditionPolicy(final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
            final ConditionsAugPolicy conditionPolicy) {
        synchronized (this.conditionsRegistry) {
            final ConditionsAugPolicy prev = this.conditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
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

    public AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<? extends Augmentation<BgpConditions>> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy conditionPolicy) {
        return this.bgpConditionsRegistry
                .registerBgpConditionsAugmentationPolicy(conditionPolicyClass, conditionPolicy);
    }

    public <T extends ChildOf<BgpMatchConditions>> AbstractRegistration registerBgpConditionsPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsPolicy<T> conditionPolicy) {
        return this.bgpConditionsRegistry
                .registerBgpConditionsPolicy(conditionPolicyClass, conditionPolicy);
    }

    boolean matchExportConditions(
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final Conditions conditions) {
        synchronized (this) {
            if (this.genericConditionHandler != null) {
                if (!this.genericConditionHandler
                        .matchExportCondition(entryInfo, routeEntryExportParameters, attributes, conditions)) {
                    return false;
                }
            }
        }

        if (!this.bgpConditionsRegistry
                .matchExportConditions(entryInfo, routeEntryExportParameters, attributes, conditions)) {
            return false;
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(conditions);

        if (conditionsAug != null) {
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
                final ConditionsAugPolicy handler = this.conditionsRegistry.get(entry.getKey());
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
            final Attributes attributes,
            final Conditions conditions) {

        synchronized (this) {
            if (this.genericConditionHandler != null) {
                if (!this.genericConditionHandler
                        .matchImportCondition(entryInfo, routeEntryImportParameters, attributes, conditions)) {
                    return false;
                }
            }
        }

        if (!this.bgpConditionsRegistry
                .matchImportConditions(entryInfo, routeEntryImportParameters, attributes, conditions)) {
            return false;
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(conditions);

        if (conditionsAug != null && attributes != null) {
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
                final ConditionsAugPolicy handler = this.conditionsRegistry.get(entry.getKey());
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
