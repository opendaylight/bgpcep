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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.ActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.IGPAugActionPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.actions.route.disposition.RejectRoute;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ActionsRegistryImpl {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<IgpActions>>, IGPAugActionPolicy> igpActionsRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Actions>>, ActionPolicy> actionsRegistry = new HashMap<>();

    AbstractRegistration registerActionPolicy(
        final Class<? extends Augmentation<Actions>> actionPolicyClass,
        final ActionPolicy actionPolicy) {
        synchronized (this.actionsRegistry) {
            final ActionPolicy prev = this.actionsRegistry.putIfAbsent(actionPolicyClass, actionPolicy);
            Preconditions.checkState(prev == null, "Action Policy %s already registered %s", actionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ActionsRegistryImpl.this.actionsRegistry) {
                        ActionsRegistryImpl.this.actionsRegistry.remove(actionPolicyClass);
                    }
                }
            };
        }
    }

    AbstractRegistration registerIGPActionPolicy(final Class<? extends Augmentation<IgpActions>> igpActionPolicyClass,
        final IGPAugActionPolicy igpActionPolicy) {
        synchronized (this.igpActionsRegistry) {
            final IGPAugActionPolicy prev = this.igpActionsRegistry.putIfAbsent(igpActionPolicyClass, igpActionPolicy);
            Preconditions.checkState(prev == null, "Action Policy %s already registered %s", igpActionPolicy, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ActionsRegistryImpl.this.igpActionsRegistry) {
                        ActionsRegistryImpl.this.igpActionsRegistry.remove(igpActionPolicyClass);
                    }
                }
            };
        }
    }

    ContainerNode applyExportAction(final long localAs, final Ipv4Address originatorId, final ClusterIdentifier clusterId,
        final PathArgument routeKey, final PeerId fromPeerId, final PeerRole fromPeerRole, final PeerId toPeer,
        final PeerRole toPeerRole, final ContainerNode attributes, final Actions actions) {
        if (actions.getRouteDisposition() instanceof RejectRoute) {
            return null;
        }
        ContainerNode attributesUpdated = applyExportIGPActions(localAs, originatorId, clusterId, routeKey, fromPeerId,
            fromPeerRole, toPeer, toPeerRole, attributes, actions.getIgpActions());

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
            .getAugmentations(actions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ActionPolicy handler = this.actionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            }
            attributesUpdated = handler.applyExportAction(localAs, originatorId,
                clusterId, routeKey, fromPeerId, fromPeerRole, toPeer, toPeerRole, attributesUpdated,
                (Augmentation<Actions>) entry.getValue());
        }
        return attributesUpdated;
    }

    private ContainerNode applyExportIGPActions(final long localAs, final Ipv4Address originatorId,
        final ClusterIdentifier clusterId, final PathArgument routeKey, final PeerId fromPeerId,
        final PeerRole fromPeerRole, final PeerId toPeer, final PeerRole toPeerRole, final ContainerNode attributes,
        final IgpActions igpActions) {
        ContainerNode attributesUpdated = attributes;
        final TagType tag = igpActions.getSetTag();
        if (tag != null) {
            //TODO Create Task for it
        }
        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
            .getAugmentations(igpActions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final IGPAugActionPolicy handler = this.igpActionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            }
            attributesUpdated = handler.apply(localAs, originatorId, clusterId,
                routeKey, fromPeerId, fromPeerRole,
                toPeer, toPeerRole, attributesUpdated, (Augmentation<IgpActions>) entry.getValue());
        }
        return attributesUpdated;
    }

    ContainerNode applyImportAction(final long localAs, final Ipv4Address originatorId, final ClusterIdentifier clusterId,
        final PathArgument routeKey, final PeerId fromPeerId, final PeerRole fromPeerRole,
        final ContainerNode attributes, final Actions actions) {
        if (actions.getRouteDisposition() instanceof RejectRoute) {
            return null;
        }
        ContainerNode attributesUpdated = applyImportIGPActions(localAs, originatorId, clusterId, routeKey,
            fromPeerId, fromPeerRole, attributes, actions.getIgpActions());

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
            .getAugmentations(actions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ActionPolicy handler = this.actionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            }
            attributesUpdated = handler.applyImportAction(localAs, originatorId, clusterId, routeKey, fromPeerId,
                fromPeerRole, attributesUpdated, (Augmentation<Actions>) entry.getValue());
        }
        return attributesUpdated;
    }

    private ContainerNode applyImportIGPActions(final long localAs, final Ipv4Address originatorId,
        final ClusterIdentifier clusterId, final PathArgument routeKey, final PeerId fromPeerId,
        final PeerRole fromPeerRole, final ContainerNode attributes, final IgpActions igpActions) {
        ContainerNode attributesUpdated = attributes;
        final TagType tag = igpActions.getSetTag();
        if (tag != null) {
            //TODO Create Task for it
        }
        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
            .getAugmentations(igpActions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final IGPAugActionPolicy handler = this.igpActionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            }
            attributesUpdated = handler.apply(localAs, originatorId, clusterId,
                routeKey, fromPeerId, fromPeerRole, attributesUpdated, (Augmentation<IgpActions>) entry.getValue());
        }
        return attributesUpdated;
    }
}
