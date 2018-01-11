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
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.RouteEntryBaseAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.ActionsAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionAugPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.policy.action.BgpActionPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryImportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.Actions1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpNextHopType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetMedType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.BgpActions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetAsPathPrepend;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpOriginAttrType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.generic.actions.route.disposition.RejectRoute;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.statement.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;

final class ActionsRegistryImpl {
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<Actions>>, ActionsAugPolicy> actionsRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends ChildOf<BgpActions>>, BgpActionPolicy> bgpActionsRegistry = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends Augmentation<BgpActions>>, BgpActionAugPolicy> bgpAugActionsRegistry
            = new HashMap<>();

    AbstractRegistration registerActionPolicy(
            final Class<? extends Augmentation<Actions>> actionPolicyClass,
            final ActionsAugPolicy actionPolicy) {
        synchronized (this.actionsRegistry) {
            final ActionsAugPolicy prev = this.actionsRegistry.putIfAbsent(actionPolicyClass, actionPolicy);
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

    public AbstractRegistration registerBgpActionPolicy(
            final Class<? extends ChildOf<BgpActions>> bgpActionPolicyClass,
            final BgpActionPolicy bgpActionPolicy) {
        synchronized (this.bgpActionsRegistry) {
            final BgpActionPolicy prev = this.bgpActionsRegistry.putIfAbsent(bgpActionPolicyClass, bgpActionPolicy);
            Preconditions.checkState(prev == null, "Action Policy %s already registered %s",
                    bgpActionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ActionsRegistryImpl.this.bgpActionsRegistry) {
                        ActionsRegistryImpl.this.bgpActionsRegistry.remove(bgpActionPolicyClass);
                    }
                }
            };
        }
    }

    public AbstractRegistration registerBgpActionAugmentationPolicy(
            final Class<? extends Augmentation<BgpActions>> bgpActionPolicyClass,
            final BgpActionAugPolicy bgpActionPolicy) {
        synchronized (this.bgpAugActionsRegistry) {
            final BgpActionAugPolicy prev = this.bgpAugActionsRegistry
                    .putIfAbsent(bgpActionPolicyClass, bgpActionPolicy);
            Preconditions.checkState(prev == null, "Action Policy %s already registered %s",
                    bgpActionPolicyClass, prev);
            return new AbstractRegistration() {
                @Override
                protected void removeRegistration() {
                    synchronized (ActionsRegistryImpl.this.bgpAugActionsRegistry) {
                        ActionsRegistryImpl.this.bgpAugActionsRegistry.remove(bgpActionPolicyClass);
                    }
                }
            };
        }
    }

    Attributes applyExportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            final Attributes attributes,
            final Actions actions) {
        requireNonNull(attributes);
        if (actions.getRouteDisposition() instanceof RejectRoute) {
            return null;
        }
        Attributes attributesUpdated = attributes;
        attributesUpdated = applyExportBGPActions(routeEntryInfo, routeEntryExportParameters,
                attributesUpdated, actions.getAugmentation(Actions1.class));
        if (attributesUpdated == null) {
            return null;
        }
        attributesUpdated = applyExportAugActions(routeEntryInfo, routeEntryExportParameters,
                attributesUpdated, actions);
        return attributesUpdated;
    }

    @SuppressWarnings("unchecked")
    private Attributes applyExportAugActions(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            @Nonnull final Attributes attributes,
            final Actions actions) {
        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(actions);

        if (conditionsAug == null) {
            return attributes;
        }
        Attributes attributesUpdated = attributes;
        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ActionsAugPolicy handler = this.actionsRegistry.get(entry.getKey());
            if (attributesUpdated == null) {
                return null;
            } else if (handler == null) {
                continue;
            }
            attributesUpdated = handler.applyExportAction(routeEntryInfo, routeEntryExportParameters,
                    attributesUpdated, (Augmentation<Actions>) entry.getValue());
        }
        return attributesUpdated;
    }

    @SuppressWarnings("unchecked")
    private Attributes applyExportBGPActions(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryExportParameters routeEntryExportParameters,
            @Nonnull final Attributes attributes,
            final Actions1 augmentation) {
        if (augmentation == null || augmentation.getBgpActions() == null) {
            return attributes;
        }
        Attributes attributesUpdated = attributes;
        final BgpActions bgpActions = augmentation.getBgpActions();

        final SetAsPathPrepend asPrependAction = bgpActions.getSetAsPathPrepend();
        final Long localPrefPrependAction = bgpActions.getSetLocalPref();
        final BgpOriginAttrType localOriginAction = bgpActions.getSetRouteOrigin();
        final BgpSetMedType medAction = bgpActions.getSetMed();
        final BgpNextHopType nhAction = bgpActions.getSetNextHop();

        if (asPrependAction != null) {
            attributesUpdated = this.bgpActionsRegistry.get(SetAsPathPrepend.class)
                    .applyExportAction(routeEntryInfo, routeEntryExportParameters, attributesUpdated, asPrependAction);
        }

        boolean updated = false;
        if (localPrefPrependAction != null || localOriginAction != null
                || medAction != null || nhAction != null) {
            updated = true;
        }

        if (updated) {
            final AttributesBuilder attributesUpdatedBuilder = new AttributesBuilder(attributes);
            if (localPrefPrependAction != null) {
                attributesUpdatedBuilder.setLocalPref(new LocalPrefBuilder().setPref(localPrefPrependAction).build());
            }

            if (localOriginAction != null) {
                attributesUpdatedBuilder.setOrigin(new OriginBuilder()
                        .setValue(BgpOrigin.forValue(localOriginAction.getIntValue())).build());
            }

            if (medAction != null) {
                attributesUpdatedBuilder.setMultiExitDisc(new MultiExitDiscBuilder()
                        .setMed(medAction.getUint32()).build());
            }

            if (nhAction != null) {
                final IpAddress address = nhAction.getIpAddress();
                if (address != null) {
                    CNextHop nhNew;
                    if (address.getIpv4Address() != null) {
                        nhNew = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                                .setGlobal(address.getIpv4Address()).build()).build();
                    } else {
                        nhNew = new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                                .setGlobal(address.getIpv6Address()).build()).build();
                    }

                    attributesUpdatedBuilder.setCNextHop(nhNew);
                }
            }
            attributesUpdated = attributesUpdatedBuilder.build();
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(bgpActions);

        if (conditionsAug != null) {
            for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
                final BgpActionAugPolicy handler = this.bgpAugActionsRegistry.get(entry.getKey());
                if (handler == null) {
                    continue;
                } else if (attributesUpdated == null) {
                    return null;
                }
                attributesUpdated = handler.applyExportAction(routeEntryInfo, routeEntryExportParameters,
                        attributesUpdated, entry.getValue());
            }
        }

        return attributesUpdated;
    }

    Attributes applyImportAction(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final Attributes attributes,
            final Actions actions) {
        requireNonNull(attributes);
        if (actions.getRouteDisposition() instanceof RejectRoute) {
            return null;
        }
        Attributes attributesUpdated = attributes;
        attributesUpdated = applyImportBGPActions(routeEntryInfo, routeBaseParameters,
                attributesUpdated, actions.getAugmentation(Actions1.class));
        if (attributesUpdated == null) {
            return null;
        }
        attributesUpdated = applyImportAugActions(routeEntryInfo, routeBaseParameters, attributesUpdated,
                actions);
        return attributesUpdated;
    }

    @SuppressWarnings("unchecked")
    private Attributes applyImportBGPActions(
            final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            final Attributes attributes,
            final Actions1 augmentation) {
        if (augmentation == null || augmentation.getBgpActions() == null) {
            return attributes;
        }
        Attributes attributesUpdated = attributes;
        final BgpActions bgpActions = augmentation.getBgpActions();

        final SetAsPathPrepend asPrependAction = bgpActions.getSetAsPathPrepend();
        if (asPrependAction != null) {
            attributesUpdated = this.bgpActionsRegistry.get(asPrependAction.getClass())
                    .applyImportAction(routeEntryInfo, routeBaseParameters, attributesUpdated, asPrependAction);
        }

        if (attributesUpdated == null) {
            return null;
        }

        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(bgpActions);

        if (conditionsAug == null) {
            return attributes;
        }

        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final BgpActionAugPolicy handler = this.bgpAugActionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            } else if (attributesUpdated == null) {
                return null;
            }
            attributesUpdated = handler.applyImportAction(routeEntryInfo, routeBaseParameters, attributesUpdated,
                    entry.getValue());
        }
        return attributesUpdated;
    }

    @SuppressWarnings("unchecked")
    private Attributes applyImportAugActions(final RouteEntryBaseAttributes routeEntryInfo,
            final BGPRouteEntryImportParameters routeBaseParameters,
            @Nonnull final Attributes attributes,
            final Actions actions) {
        final Map<Class<? extends Augmentation<?>>, Augmentation<?>> conditionsAug = BindingReflections
                .getAugmentations(actions);

        if (conditionsAug == null) {
            return attributes;
        }
        Attributes attributesUpdated = attributes;
        for (final Map.Entry<Class<? extends Augmentation<?>>, Augmentation<?>> entry : conditionsAug.entrySet()) {
            final ActionsAugPolicy handler = this.actionsRegistry.get(entry.getKey());
            if (handler == null) {
                continue;
            } else if (attributesUpdated == null) {
                return null;
            }
            attributesUpdated = handler.applyImportAction(routeEntryInfo, routeBaseParameters, attributesUpdated,
                    (Augmentation<Actions>) entry.getValue());
        }
        return attributesUpdated;
    }
}
