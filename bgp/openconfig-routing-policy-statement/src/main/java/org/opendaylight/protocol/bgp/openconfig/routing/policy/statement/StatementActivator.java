/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.AbstractBGPStatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.FromExternalImportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.FromInternalImportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ToExternalExportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ToInternalExportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ToOdlInternalExportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ToReflectorClientExportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.NeighborAnnounced;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.NeighborAnnouncer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.FromExternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.FromInternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.FromOdlInternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.FromRouteReflectorImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.NeighborAnnouncedCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.NeighborAnnouncerCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.ToExternalExportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.ToInternalExportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.ToOdlInternalExportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev170109.ToRouteReflectorExportActions;

public final class StatementActivator extends AbstractBGPStatementProviderActivator {
    @Override
    protected synchronized List<AutoCloseable> startImpl(final StatementRegistryProvider provider) {
        final List<AutoCloseable> registration =  new ArrayList<>(10);

        registerActions(provider, registration);
        registerConditions(provider, registration);

        return registration;
    }

    private void registerConditions(final StatementRegistryProvider provider, final List<AutoCloseable> registration) {
        final NeighborAnnounced neighborAnnounced = new NeighborAnnounced();
        registration.add(provider.registerConditionPolicy(NeighborAnnouncedCondition.class, neighborAnnounced));
        final NeighborAnnouncer neighborAnnouncer = new NeighborAnnouncer();
        registration.add(provider.registerConditionPolicy(NeighborAnnouncerCondition.class, neighborAnnouncer));
    }

    private void registerActions(final StatementRegistryProvider provider, final List<AutoCloseable> registration) {
        final ToExternalExportPolicy toExternalExportPolicy = new ToExternalExportPolicy();
        registration.add(provider.registerActionPolicy(ToExternalExportActions.class,
            toExternalExportPolicy));

        final ToInternalExportPolicy toInternalExportPolicy = new ToInternalExportPolicy();
        registration.add(provider.registerActionPolicy(ToInternalExportActions.class,
            toInternalExportPolicy));

        final ToOdlInternalExportPolicy toOdlInternalExportPolicy = new ToOdlInternalExportPolicy();
        registration.add(provider.registerActionPolicy(ToOdlInternalExportActions.class,
            toOdlInternalExportPolicy));

        final ToReflectorClientExportPolicy toRRExportPolicy = new ToReflectorClientExportPolicy();
        registration.add(provider.registerActionPolicy(ToRouteReflectorExportActions.class,
            toRRExportPolicy));

        final FromExternalImportPolicy fromExternalImportPolicy = new FromExternalImportPolicy();
        registration.add(provider.registerActionPolicy(FromExternalImportActions.class,
            fromExternalImportPolicy));

        final FromInternalImportPolicy fromInternalImportPolicy = new FromInternalImportPolicy();
        registration.add(provider.registerActionPolicy(FromInternalImportActions.class,
            fromInternalImportPolicy));

        // Same behaviour for ODL Internal Peer
        registration.add(provider.registerActionPolicy(FromOdlInternalImportActions.class,
            fromInternalImportPolicy));

        // Same behaviour for Route Reflector Peer
        registration.add(provider.registerActionPolicy(FromRouteReflectorImportActions.class,
            fromInternalImportPolicy));
    }
}
