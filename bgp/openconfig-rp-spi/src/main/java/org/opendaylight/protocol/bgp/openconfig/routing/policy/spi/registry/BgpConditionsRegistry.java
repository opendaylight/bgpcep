/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Conditions1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public final class BgpConditionsRegistry {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<BgpConditions>>,
            BgpConditionsAugmentationPolicy> bgpConditionsRegistry = new HashMap<>();

    public AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<? extends Augmentation<BgpConditions>> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy conditionPolicy) {
        synchronized (this.bgpConditionsRegistry) {
            final BgpConditionsAugmentationPolicy prev
                    = this.bgpConditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (BgpConditionsRegistry.this.bgpConditionsRegistry) {
                        BgpConditionsRegistry.this.bgpConditionsRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    public boolean matchExportConditions(
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final Conditions conditions) {
        final Conditions1 bgpConditionsAug = conditions.getAugmentation(Conditions1.class);
        if (bgpConditionsAug != null) {
            final BgpConditions bgpConditions = bgpConditionsAug.getBgpConditions();
            synchronized (this) {
                if (!matchExportCondition(entryInfo, routeEntryExportParameters, attributes,
                        bgpConditions)) {
                    return false;
                }
            }
            final Map<Class<? extends Augmentation<?>>, Augmentation<?>> bgpAug = BindingReflections
                    .getAugmentations(bgpConditions);
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : bgpAug.entrySet()) {
                final BgpConditionsAugmentationPolicy handler = this.bgpConditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchExportCondition(entryInfo, routeEntryExportParameters, attributes,
                        entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean matchImportConditions(
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final Conditions conditions) {

        final Conditions1 bgpConditionsAug = conditions.getAugmentation(Conditions1.class);
        if (bgpConditionsAug != null) {
            final BgpConditions bgpConditions = bgpConditionsAug.getBgpConditions();
            synchronized (this) {
                if (!matchImportCondition(entryInfo, routeEntryImportParameters, attributes,
                        bgpConditions)) {
                    return false;
                }
            }
            final Map<Class<? extends Augmentation<?>>, Augmentation<?>> bgpAug = BindingReflections
                    .getAugmentations(bgpConditions);
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : bgpAug.entrySet()) {
                final BgpConditionsAugmentationPolicy handler = this.bgpConditionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchImportCondition(entryInfo, routeEntryImportParameters, attributes,
                        entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final BgpConditions conditions) {
        //TBD
        return true;
    }

    public boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final BgpConditions conditions) {
        //TBD
        return true;
    }
}
