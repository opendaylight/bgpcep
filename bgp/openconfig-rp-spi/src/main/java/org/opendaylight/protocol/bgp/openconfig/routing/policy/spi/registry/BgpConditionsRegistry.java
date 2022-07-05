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
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Conditions1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchAsPathSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;

final class BgpConditionsRegistry {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<BgpConditions>>,
            BgpConditionsAugmentationPolicy> bgpConditionsAugRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends ChildOf<BgpMatchConditions>>,
            BgpConditionsPolicy> bgpConditionsRegistry = new HashMap<>();

    AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<? extends Augmentation<BgpConditions>> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy conditionPolicy) {
        synchronized (bgpConditionsAugRegistry) {
            final BgpConditionsAugmentationPolicy prev
                    = bgpConditionsAugRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (bgpConditionsAugRegistry) {
                        bgpConditionsAugRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    <T extends ChildOf<BgpMatchConditions>, N> AbstractRegistration registerBgpConditionsPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsPolicy<T, N> conditionPolicy) {
        synchronized (bgpConditionsRegistry) {
            final BgpConditionsPolicy prev
                    = bgpConditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (bgpConditionsRegistry) {
                        bgpConditionsRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    boolean matchExportConditions(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final Conditions conditions) {
        final Conditions1 bgpConditionsAug = conditions.augmentation(Conditions1.class);
        if (bgpConditionsAug != null) {

            final BgpConditions bgpConditions = bgpConditionsAug.getBgpConditions();
            if (!matchExportCondition(afiSafi, entryInfo, routeEntryExportParameters, attributes,
                    bgpConditions)) {
                return false;
            }
            for (final Augmentation<BgpConditions> condition : bgpConditions.augmentations().values()) {
                final BgpConditionsAugmentationPolicy handler =
                    bgpConditionsAugRegistry.get(condition.implementedInterface());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchExportCondition(afiSafi, entryInfo, routeEntryExportParameters,
                        handler.getConditionParameter(attributes), condition)) {
                    return false;
                }
            }
        }
        return true;
    }


    boolean matchImportConditions(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes entryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final Conditions conditions) {

        final Conditions1 bgpConditionsAug = conditions.augmentation(Conditions1.class);
        if (bgpConditionsAug != null) {
            final BgpConditions bgpConditions = bgpConditionsAug.getBgpConditions();
            synchronized (this) {
                if (!matchImportCondition(afiSafi, entryInfo, routeEntryImportParameters, attributes,
                        bgpConditions)) {
                    return false;
                }
            }
            for (final Augmentation<BgpConditions> condition : bgpConditions.augmentations().values()) {
                final BgpConditionsAugmentationPolicy handler =
                    bgpConditionsAugRegistry.get(condition.implementedInterface());
                if (handler == null) {
                    continue;
                }
                if (!handler.matchImportCondition(afiSafi, entryInfo, routeEntryImportParameters,
                        handler.getConditionParameter(attributes), condition)) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean matchImportCondition(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final BgpConditions conditions) {

        if (!BgpAttributeConditionsUtil.matchConditions(afiSafi, attributes, conditions)) {
            return false;
        }

        final MatchCommunitySet matchCond = conditions.getMatchCommunitySet();
        if (matchCond != null) {
            final BgpConditionsPolicy handler = bgpConditionsRegistry.get(MatchCommunitySet.class);
            if (!handler.matchImportCondition(afiSafi, routeEntryInfo, routeEntryImportParameters,
                    handler.getConditionParameter(attributes), matchCond)) {
                return false;
            }
        }

        final MatchAsPathSet matchAsPathSet = conditions.getMatchAsPathSet();
        if (matchCond != null) {
            final BgpConditionsPolicy handler = bgpConditionsRegistry.get(MatchAsPathSet.class);
            if (!handler.matchImportCondition(afiSafi, routeEntryInfo, routeEntryImportParameters,
                    handler.getConditionParameter(attributes), matchAsPathSet)) {
                return false;
            }
        }

        final MatchExtCommunitySet matchExtCommSet = conditions.getMatchExtCommunitySet();
        if (matchExtCommSet != null) {
            final BgpConditionsPolicy handler = bgpConditionsRegistry.get(MatchAsPathSet.class);
            if (!handler.matchImportCondition(afiSafi, routeEntryInfo, routeEntryImportParameters,
                    handler.getConditionParameter(attributes), matchExtCommSet)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean matchExportCondition(
            final AfiSafiType afiSafi,
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final BgpConditions conditions) {
        if (!BgpAttributeConditionsUtil.matchConditions(afiSafi, attributes, conditions)) {
            return false;
        }

        final MatchCommunitySet matchCond = conditions.getMatchCommunitySet();
        if (matchCond != null) {
            final BgpConditionsPolicy handler = bgpConditionsRegistry.get(MatchCommunitySet.class);
            if (!handler.matchExportCondition(afiSafi, routeEntryInfo, routeEntryExportParameters,
                    handler.getConditionParameter(attributes), matchCond)) {
                return false;
            }
        }

        final MatchAsPathSet matchAsPathSet = conditions.getMatchAsPathSet();
        if (matchAsPathSet != null) {
            final BgpConditionsPolicy handler = bgpConditionsRegistry.get(MatchAsPathSet.class);
            if (!handler.matchExportCondition(afiSafi, routeEntryInfo, routeEntryExportParameters,
                    handler.getConditionParameter(attributes), matchAsPathSet)) {
                return false;
            }
        }

        final MatchExtCommunitySet matchExtCommSet = conditions.getMatchExtCommunitySet();
        if (matchExtCommSet != null) {
            final BgpConditionsPolicy handler = bgpConditionsRegistry.get(MatchExtCommunitySet.class);
            if (!handler.matchExportCondition(afiSafi, routeEntryInfo, routeEntryExportParameters,
                    handler.getConditionParameter(attributes), matchExtCommSet)) {
                return false;
            }
        }

        return true;
    }
}
