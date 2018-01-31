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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsAugmentationPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.condition.BgpConditionsPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpMatchConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Conditions1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.attribute.conditions.AsPathLength;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchAsPathSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.bgp.match.conditions.MatchExtCommunitySet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.conditions.BgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpOriginAttrType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeComparison;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeEq;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeGe;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.AttributeLe;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AsPathSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.EmptyNextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

public final class BgpConditionsRegistry {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<BgpConditions>>,
            BgpConditionsAugmentationPolicy> bgpConditionsAugRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends ChildOf<BgpMatchConditions>>,
            BgpConditionsPolicy> bgpConditionsRegistry = new HashMap<>();

    public AbstractRegistration registerBgpConditionsAugmentationPolicy(
            final Class<? extends Augmentation<BgpConditions>> conditionPolicyClass,
            final BgpConditionsAugmentationPolicy conditionPolicy) {
        synchronized (this.bgpConditionsAugRegistry) {
            final BgpConditionsAugmentationPolicy prev
                    = this.bgpConditionsAugRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s",
                    conditionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (BgpConditionsRegistry.this.bgpConditionsAugRegistry) {
                        BgpConditionsRegistry.this.bgpConditionsAugRegistry.remove(conditionPolicyClass);
                    }
                }
            };
        }
    }

    public <T extends ChildOf<BgpMatchConditions>> AbstractRegistration registerBgpConditionsPolicy(
            final Class<T> conditionPolicyClass,
            final BgpConditionsPolicy<T> conditionPolicy) {
        synchronized (this.bgpConditionsRegistry) {
            final BgpConditionsPolicy prev
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

    @SuppressWarnings("unchecked")
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
                final BgpConditionsAugmentationPolicy handler = this.bgpConditionsAugRegistry.get(entry.getKey());
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
                final BgpConditionsAugmentationPolicy handler = this.bgpConditionsAugRegistry.get(entry.getKey());
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

    @SuppressWarnings("unchecked")
    private boolean matchImportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeEntryImportParameters,
            final Attributes attributes,
            final BgpConditions conditions) {

        if (!matchAsPathLength(attributes.getAsPath(), conditions.getAsPathLength())) {
            return false;
        }

        if (!matchMED(attributes.getMultiExitDisc(), conditions.getMedEq())) {
            return false;
        }

        if (!matchOrigin(attributes.getOrigin(), conditions.getOriginEq())) {
            return false;
        }

        if (!matchNextHopIn(attributes.getCNextHop(), conditions.getNextHopIn())) {
            return false;
        }

        if (!matchLocalPref(attributes.getLocalPref(), conditions.getLocalPrefEq())) {
            return false;
        }

        final MatchCommunitySet matchCond = conditions.getMatchCommunitySet();
        if (matchCond != null) {
            if (!this.bgpConditionsRegistry.get(MatchCommunitySet.class)
                    .matchImportCondition(routeEntryInfo, routeEntryImportParameters, attributes, matchCond)) {
                return false;
            }
        }

        final MatchAsPathSet matchAsPathSet = conditions.getMatchAsPathSet();
        if (matchCond != null) {
            if (!this.bgpConditionsRegistry.get(MatchAsPathSet.class)
                    .matchImportCondition(routeEntryInfo, routeEntryImportParameters, attributes, matchAsPathSet)) {
                return false;
            }
        }

        final MatchExtCommunitySet matchExtCommSet = conditions.getMatchExtCommunitySet();
        if (matchExtCommSet != null) {
            if (!this.bgpConditionsRegistry.get(MatchExtCommunitySet.class)
                    .matchImportCondition(routeEntryInfo, routeEntryImportParameters, attributes, matchExtCommSet)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean matchExportCondition(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final BgpConditions conditions) {
        if (!matchAsPathLength(attributes.getAsPath(), conditions.getAsPathLength())) {
            return false;
        }

        if (!matchMED(attributes.getMultiExitDisc(), conditions.getMedEq())) {
            return false;
        }

        if (!matchOrigin(attributes.getOrigin(), conditions.getOriginEq())) {
            return false;
        }

        if (!matchNextHopIn(attributes.getCNextHop(), conditions.getNextHopIn())) {
            return false;
        }

        if (!matchLocalPref(attributes.getLocalPref(), conditions.getLocalPrefEq())) {
            return false;
        }

        final MatchCommunitySet matchCond = conditions.getMatchCommunitySet();
        if (matchCond != null) {
            if (!this.bgpConditionsRegistry.get(MatchCommunitySet.class)
                    .matchExportCondition(routeEntryInfo, routeEntryExportParameters, attributes, matchCond)) {
                return false;
            }
        }

        final MatchAsPathSet matchAsPathSet = conditions.getMatchAsPathSet();
        if (matchAsPathSet != null) {
            if (!this.bgpConditionsRegistry.get(MatchAsPathSet.class)
                    .matchExportCondition(routeEntryInfo, routeEntryExportParameters, attributes, matchAsPathSet)) {
                return false;
            }
        }

        final MatchExtCommunitySet matchExtCommSet = conditions.getMatchExtCommunitySet();
        if (matchExtCommSet != null) {
            if (!this.bgpConditionsRegistry.get(MatchExtCommunitySet.class)
                    .matchExportCondition(routeEntryInfo, routeEntryExportParameters, attributes, matchExtCommSet)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchMED(final MultiExitDisc multiExitDisc, final Long med) {
        if (multiExitDisc == null || med == null) {
            return true;
        }

        return multiExitDisc.getMed().equals(med);
    }

    private boolean matchOrigin(final Origin origin, final BgpOriginAttrType originEq) {
        if (origin == null || originEq == null) {
            return true;
        }
        return origin.getValue().getIntValue() == originEq.getIntValue();
    }

    private boolean matchAsPathLength(final AsPath asPath, final AsPathLength asPathLength) {
        if (asPath == null || asPathLength == null) {
            return true;
        }

        final List<Segments> segments = asPath.getSegments();
        int total = segments.stream().map(AsPathSegment::getAsSequence)
                .filter(Objects::nonNull).mapToInt(List::size).sum();

        if (total == 0) {
            total = segments.stream().map(AsPathSegment::getAsSet)
                    .filter(Objects::nonNull).mapToInt(List::size).sum();
        }

        final Class<? extends AttributeComparison> comp = asPathLength.getOperator();
        final long asPathLenght = asPathLength.getValue();
        if (comp == AttributeEq.class) {
            return total == asPathLenght;
        } else if (comp == AttributeGe.class) {
            return total >= asPathLenght;
        } else if (comp == AttributeLe.class) {
            return total <= asPathLenght;
        }
        return false;
    }


    private boolean matchNextHopIn(final CNextHop nextHop, final List<IpAddress> nextHopIn) {
        if (nextHop == null || nextHopIn == null || nextHop instanceof EmptyNextHopCase) {
            return true;
        }

        IpAddress global;
        if (nextHop instanceof Ipv4NextHopCase) {
            global = new IpAddress(((Ipv4NextHopCase) nextHop).getIpv4NextHop().getGlobal());
        } else {
            global = new IpAddress(((Ipv6NextHopCase) nextHop).getIpv6NextHop().getGlobal());
        }
        return nextHopIn.contains(global);
    }

    private boolean matchLocalPref(final LocalPref localPref, final Long localPrefEq) {
        if (localPref == null || localPrefEq == null) {
            return true;
        }
        return localPref.getPref().equals(localPrefEq);
    }
}
