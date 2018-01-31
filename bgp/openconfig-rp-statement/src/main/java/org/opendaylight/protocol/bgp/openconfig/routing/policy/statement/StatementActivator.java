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
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.AsPathPrependAction;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.NonTransitiveAttributesFilterAction;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.SetClusterIdPrependAction;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.actions.SetOriginatorIdPrependAction;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.GenericConditionPolicyHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchClusterIdSetConditionHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchOriginatorIdSetConditionHandler;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions.MatchRoleSetConditionHandler;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.SetAsPathPrepend;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.MatchClusterIdSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.MatchOriginatorIdSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.MatchRoleSetCondition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.NonTransitiveAttributesFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.SetClusterIdPrepend;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.SetOriginatorIdPrepend;

public final class StatementActivator extends AbstractBGPStatementProviderActivator {
    private final DataBroker dataBroker;

    public StatementActivator(final DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    @Override
    protected synchronized List<AutoCloseable> startImpl(final StatementRegistryProvider provider) {
        final List<AutoCloseable> registration = new ArrayList<>(8);
        registerActions(provider, registration);
        registerConditions(provider, registration);
        return registration;
    }

    private void registerConditions(final StatementRegistryProvider provider, final List<AutoCloseable> registration) {
        registration.add(provider.registerBgpConditionsAugmentationPolicy(MatchRoleSetCondition.class,
                new MatchRoleSetConditionHandler(this.dataBroker)));

        registration.add(provider.registerBgpConditionsAugmentationPolicy(MatchOriginatorIdSetCondition.class,
                new MatchOriginatorIdSetConditionHandler(this.dataBroker)));

        registration.add(provider.registerBgpConditionsAugmentationPolicy(MatchClusterIdSetCondition.class,
                new MatchClusterIdSetConditionHandler(this.dataBroker)));

        registration.add(provider.registerGenericConditionPolicy(new GenericConditionPolicyHandler(this.dataBroker)));
    }

    private void registerActions(final StatementRegistryProvider provider, final List<AutoCloseable> registration) {
        registration.add(provider.registerBgpActionPolicy(SetAsPathPrepend.class, new AsPathPrependAction()));

        registration.add(provider.registerBgpActionAugmentationPolicy(SetOriginatorIdPrepend.class,
                new SetOriginatorIdPrependAction()));

        registration.add(provider.registerBgpActionAugmentationPolicy(NonTransitiveAttributesFilter.class,
                new NonTransitiveAttributesFilterAction()));

        registration.add(provider.registerBgpActionAugmentationPolicy(SetClusterIdPrepend.class,
                new SetClusterIdPrependAction()));
    }
}
