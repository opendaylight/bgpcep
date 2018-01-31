/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.FromNonExternalImportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ReflectAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ReflectAttributesFromInternal;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ToExternalExportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.NeighborAnnounced;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.NeighborAnnouncer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.FromExternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.FromNonExternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.NeighborAnnouncedCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.NeighborAnnouncerCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.ReflectAttributesActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.ReflectAttributesFromOdlInternalActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.ToExternalExportActions;

public final class StatementActivator extends AbstractBGPStatementProviderActivator {
    @Override
    protected synchronized List<AutoCloseable> startImpl(final StatementRegistryProvider provider) {
        final List<AutoCloseable> registration = new ArrayList<>(7);
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

        final FromExternalImportPolicy fromExternalImportPolicy = new FromExternalImportPolicy();
        registration.add(provider.registerActionPolicy(FromExternalImportActions.class,
                fromExternalImportPolicy));

        final FromNonExternalImportPolicy fromNonExtImportImportPolicy = new FromNonExternalImportPolicy();
        registration.add(provider.registerActionPolicy(FromNonExternalImportActions.class,
                fromNonExtImportImportPolicy));

        final ReflectAttributes reflectedAttributes = new ReflectAttributes();
        registration.add(provider.registerActionPolicy(ReflectAttributesActions.class,
                reflectedAttributes));

        final ReflectAttributesFromInternal reflectAttributesFromInternal = new ReflectAttributesFromInternal();
        registration.add(provider.registerActionPolicy(ReflectAttributesFromOdlInternalActions.class,
                reflectAttributesFromInternal));
    }
}
