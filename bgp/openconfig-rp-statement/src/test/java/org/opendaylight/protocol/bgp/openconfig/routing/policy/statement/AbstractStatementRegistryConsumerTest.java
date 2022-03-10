/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.AbstractStatementRegistryTest;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinition;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.PolicyDefinitionKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.Statements;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.policy.definitions.policy.definition.statements.Statement;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractStatementRegistryConsumerTest extends AbstractStatementRegistryTest {
    protected final StatementRegistry statementRegistry  = new StatementRegistry();

    private StatementActivator activator;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        activator = new StatementActivator(getDataBroker());
        activator.start(statementRegistry);
    }

    @After
    public void tearDown() throws Exception {
        activator.stop();
    }

    @Override
    protected List<Statement> loadStatement(final String policyName) throws ExecutionException, InterruptedException {
        final ListenableFuture<Optional<Statements>> future;
        try (ReadTransaction rt = getDataBroker().newReadOnlyTransaction()) {
            future = rt.read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build()
                    .child(PolicyDefinitions.class)
                    .child(PolicyDefinition.class, new PolicyDefinitionKey(policyName))
                    .child(Statements.class));
        }
        return future.get().orElseThrow().getStatement();
    }
}
