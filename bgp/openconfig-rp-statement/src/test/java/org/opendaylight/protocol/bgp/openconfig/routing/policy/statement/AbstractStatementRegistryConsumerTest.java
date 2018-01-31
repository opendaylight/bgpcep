/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement;

import org.junit.Before;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.impl.AbstractRoutingPolicyProviderTest;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;

public class AbstractStatementRegistryConsumerTest extends AbstractRoutingPolicyProviderTest {
    protected StatementRegistry statementRegistry;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.statementRegistry = new StatementRegistry();
        final StatementActivator activator = new StatementActivator();
        activator.start(this.statementRegistry);
    }
}
