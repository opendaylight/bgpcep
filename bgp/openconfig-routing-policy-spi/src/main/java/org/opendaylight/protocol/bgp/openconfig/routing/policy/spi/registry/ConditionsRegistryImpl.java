/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.ConditionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.IGPConditionPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.conditions.MatchPrefixSet;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.conditions.IgpConditions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Conditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.routes.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ConditionsRegistryImpl {

    private static final QName IPV4_PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final QName IPV6_PREFIX_QNAME = QName.create(Ipv6Route.QNAME, "prefix").intern();

    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Conditions>>, ConditionPolicy> conditionsRegistry = new HashMap<>();

    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy
        .rev151009.igp.conditions.IgpConditions>>, IGPConditionPolicy> igpConditionsRegistry = new HashMap<>();

    AbstractRegistration registerConditionPolicy(final Class<? extends Augmentation<Conditions>> conditionPolicyClass,
        final ConditionPolicy conditionPolicy) {
        synchronized (this.conditionsRegistry) {
            final ConditionPolicy prev = this.conditionsRegistry.putIfAbsent(conditionPolicyClass, conditionPolicy);
            Preconditions.checkState(prev == null, "Condition Policy %s already registered %s", conditionPolicyClass, prev);
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

    AbstractRegistration registerIGPConditionPolicy(final Class<? extends Augmentation<IgpConditions>> igpConditionClass,
        final IGPConditionPolicy igpConditionPolicy) {
        synchronized (this.igpConditionsRegistry) {
            final IGPConditionPolicy prev = this.igpConditionsRegistry
                .putIfAbsent(igpConditionClass, igpConditionPolicy);
            Preconditions.checkState(prev == null, "IGP Condition Policy %s already registered %s", igpConditionPolicy,
                prev);
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

    boolean matchExportConditions(final OpenconfigPolicyConsumer policyProvider, final PathArgument routeKey,
        final PeerId fromPeerId, final PeerRole fromPeerRole, final PeerId toPeer, final PeerRole toPeerRole,
        final ContainerNode attributes, final Conditions conditions) {
        final IpPrefix prefix = extractPrefix(routeKey);
        if (prefix != null && !policyProvider.matchPrefix(conditions.getMatchPrefixSet().getPrefixSet(), prefix)) {
            return false;
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> igpAug = BindingReflections
            .getAugmentations(conditions.getIgpConditions());
        if (attributes != null) {
            return true;
        }
        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : igpAug.entrySet()) {
            final IGPConditionPolicy handler = this.igpConditionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            }
            if (!handler.match(routeKey, fromPeerId, fromPeerRole, toPeer,
                toPeerRole, attributes, (Augmentation<IGPConditionPolicy>) entry.getValue())) {
                return false;
            }
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
            .getAugmentations(conditions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ConditionPolicy handler = this.conditionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            }
            if (!handler.matchExportCondition(routeKey, fromPeerId, fromPeerRole, toPeer,
                toPeerRole, attributes, (Augmentation<Conditions>) entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    boolean matchImportConditions(final OpenconfigPolicyConsumer policyProvider, final PathArgument routeKey,
        final PeerId fromPeerId, final PeerRole fromPeerRole, final ContainerNode attributes,
        final Conditions conditions) {
        if (!matchPrefix(policyProvider, routeKey, conditions.getMatchPrefixSet())) {
            return false;
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
                if (!handler.match(routeKey, fromPeerId, fromPeerRole, attributes,
                    (Augmentation<IGPConditionPolicy>) entry.getValue())) {
                    return false;
                }
            }
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
            .getAugmentations(conditions);

        if (conditionsAug != null && attributes!= null) {
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
                final ConditionPolicy handler = this.conditionsRegistry.get(entry.getKey());
                if (handler != null) {
                    final Augmentation<Conditions> conditionConfig = (Augmentation<Conditions>) entry.getValue();
                    if (!handler.matchImportCondition(routeKey, fromPeerId, fromPeerRole, attributes,
                        conditionConfig)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean matchPrefix(final OpenconfigPolicyConsumer policyProvider, final PathArgument routeKey,
        final MatchPrefixSet matchPrefixSet) {
        if (matchPrefixSet == null) {
            return true;
        }
        final IpPrefix prefix = extractPrefix(routeKey);
        return prefix != null && policyProvider.matchPrefix(matchPrefixSet.getPrefixSet(), prefix);
    }

    private IpPrefix extractPrefix(final PathArgument routeKey) {
        final QName qName = routeKey.getNodeType();
        if (Ipv4Route.QNAME.equals(qName)) {
            final Map<QName, Object> values = ((YangInstanceIdentifier.NodeIdentifierWithPredicates) routeKey).getKeyValues();
            return new IpPrefix(new Ipv4Prefix((String) values.get(IPV4_PREFIX_QNAME)));
        } else if (Ipv6Route.QNAME.equals(qName)) {
            final Map<QName, Object> values = ((YangInstanceIdentifier.NodeIdentifierWithPredicates) routeKey).getKeyValues();
            return new IpPrefix(new Ipv6Prefix((String) values.get(IPV6_PREFIX_QNAME)));
        }
        return null;
    }
}
