/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.AbstractStatementRegistryTest;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractStatementRegistryConsumerTest extends AbstractStatementRegistryTest {
    protected StatementRegistry statementRegistry;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.statementRegistry = new StatementRegistry();
        final StatementActivator activator = new StatementActivator(getDataBroker());
        activator.start(this.statementRegistry);
    }

    @Override
    protected List<Statement> loadStatement(final String policyName) throws ExecutionException, InterruptedException {
        final ReadWriteTransaction rt = getDataBroker().newReadWriteTransaction();
        final PolicyDefinition policy = rt.read(LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.create(RoutingPolicy.class).child(PolicyDefinitions.class)
                .child(PolicyDefinition.class, new PolicyDefinitionKey(policyName))).get().get();
        return policy.getStatements().getStatement();
    }
}
