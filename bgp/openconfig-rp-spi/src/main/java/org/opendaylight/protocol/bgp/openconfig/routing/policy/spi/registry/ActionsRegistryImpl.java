/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryInfo;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.IgpActionPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.actions.route.disposition.RejectRoute;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.igp.actions.IgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ActionsRegistryImpl {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<IgpActions>>, IgpActionPolicy> igpActionsRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Actions>>, ActionPolicy> actionsRegistry = new HashMap<>();

    AbstractRegistration registerActionPolicy(
            final Class<? extends Augmentation<Actions>> actionPolicyClass,
            final ActionPolicy actionPolicy) {
        synchronized (this.actionsRegistry) {
            final ActionPolicy prev = this.actionsRegistry.putIfAbsent(actionPolicyClass, actionPolicy);
            Preconditions.checkState(prev == null, "Action Policy %s already registered %s",
                    actionPolicyClass, prev);
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
            final IgpActionPolicy igpActionPolicy) {
        synchronized (this.igpActionsRegistry) {
            final IgpActionPolicy prev = this.igpActionsRegistry.putIfAbsent(igpActionPolicyClass, igpActionPolicy);
            Preconditions.checkState(prev == null, "Action Policy %s already registered %s",
                    igpActionPolicy, prev);
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

    ContainerNode applyExportAction(
            final RouteEntryInfo routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final ContainerNode attributes,
            final Actions actions) {
        requireNonNull(attributes);
        if (actions.getRouteDisposition() instanceof RejectRoute) {
            return null;
        }
        ContainerNode attributesUpdated = applyExportIGPActions(routeEntryInfo, routeEntryExportParameters,
                attributes, actions.getIgpActions());

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(actions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ActionPolicy handler = this.actionsRegistry.get(entry.getKey());
            if (attributesUpdated == null) {
                return attributesUpdated;
            } else if (handler == null) {
                continue;
            }
            attributesUpdated = handler.applyExportAction(routeEntryInfo, routeEntryExportParameters,
                    attributesUpdated, (Augmentation<Actions>) entry.getValue());

        }
        return attributesUpdated;
    }

    private ContainerNode applyExportIGPActions(
            final RouteEntryInfo routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final ContainerNode attributes,
            final IgpActions igpActions) {
        ContainerNode attributesUpdated = attributes;
        final TagType tag = igpActions.getSetTag();
        if (tag != null) {
            //TODO
        }
        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(igpActions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final IgpActionPolicy handler = this.igpActionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            } else if (attributesUpdated == null) {
                return attributesUpdated;
            }
            attributesUpdated = handler.applyExportAction(routeEntryInfo, routeEntryExportParameters,
                    attributesUpdated, (Augmentation<IgpActions>) entry.getValue());
        }
        return attributesUpdated;
    }

    ContainerNode applyImportAction(
            final RouteEntryInfo routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final ContainerNode attributes,
            final Actions actions) {
        requireNonNull(attributes);
        if (actions.getRouteDisposition() instanceof RejectRoute) {
            return null;
        }
        ContainerNode attributesUpdated = applyImportIGPActions(routeEntryInfo, routeBaseParameters, attributes,
                actions.getIgpActions());

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(actions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ActionPolicy handler = this.actionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            } else if (attributesUpdated == null) {
                return attributesUpdated;
            }
            attributesUpdated = handler.applyImportAction(routeEntryInfo, routeBaseParameters, attributesUpdated,
                    (Augmentation<Actions>) entry.getValue());
        }
        return attributesUpdated;
    }

    private ContainerNode applyImportIGPActions(
            final RouteEntryInfo routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final ContainerNode attributes,
            final IgpActions igpActions) {
        ContainerNode attributesUpdated = attributes;
        final TagType tag = igpActions.getSetTag();
        if (tag != null) {
            //TODO
        }
        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug
                = BindingReflections.getAugmentations(igpActions);

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final IgpActionPolicy handler = this.igpActionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            } else if (attributesUpdated == null) {
                return attributesUpdated;
            }
            attributesUpdated = handler.applyImportAction(routeEntryInfo, routeBaseParameters,
                    attributesUpdated, (Augmentation<IgpActions>) entry.getValue());
        }
        return attributesUpdated;
    }
}
