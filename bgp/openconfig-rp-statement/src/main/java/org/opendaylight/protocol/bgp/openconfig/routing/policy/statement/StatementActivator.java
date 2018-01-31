/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.AbstractBGPStatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.FromExternalImportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.FromNonExternalImportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ReflectAttributes;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ReflectAttributesFromInternal;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.ToExternalExportPolicy;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.GenericConditionPolicyHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchRoleSetConditionHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.FromExternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.FromNonExternalImportActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.MatchRoleSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.ReflectAttributesActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.ReflectAttributesFromOdlInternalActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.ToExternalExportActions;

public final class StatementActivator extends AbstractBGPStatementProviderActivator {
    private final DataBroker dataBroker;

    public StatementActivator(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    @Override
    protected synchronized List<AutoCloseable> startImpl(final StatementRegistryProvider provider) {
        final List<AutoCloseable> registration = new ArrayList<>(7);
        registerActions(provider, registration);
        registerConditions(provider, registration);
        return registration;
    }

    private void registerConditions(final StatementRegistryProvider provider, final List<AutoCloseable> registration) {
        registration.add(provider.registerBgpConditionPolicy(MatchRoleSetCondition.class,
                new MatchRoleSetConditionHandler(this.dataBroker)));
        registration.add(provider.registerGenericConditionPolicy(new GenericConditionPolicyHandler(this.dataBroker)));
    }

    private void registerActions(final StatementRegistryProvider provider, final List<AutoCloseable> registration) {
        registration.add(provider.registerActionPolicy(ToExternalExportActions.class, new ToExternalExportPolicy()));

        registration.add(provider.registerActionPolicy(FromExternalImportActions.class,
                new FromExternalImportPolicy()));

        registration.add(provider.registerActionPolicy(FromNonExternalImportActions.class,
                new FromNonExternalImportPolicy()));

        registration.add(provider.registerActionPolicy(ReflectAttributesActions.class, new ReflectAttributes()));

        registration.add(provider.registerActionPolicy(ReflectAttributesFromOdlInternalActions.class,
                new ReflectAttributesFromInternal()));
    }
}
