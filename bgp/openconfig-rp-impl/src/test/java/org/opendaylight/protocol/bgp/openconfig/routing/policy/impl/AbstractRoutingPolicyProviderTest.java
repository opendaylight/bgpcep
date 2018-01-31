/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.bgpcep.config.loader.routing.policy.AbstractOpenconfigRoutingPolicyLoaderTest;

public class AbstractRoutingPolicyProviderTest extends AbstractOpenconfigRoutingPolicyLoaderTest {
    protected OpenconfigRoutingPolicy policyConsumer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.policyConsumer = new OpenconfigRoutingPolicy(getDataBroker());
        this.policyConsumer.init();
    }

    @After
    public void tearDown() throws Exception {
        if (this.policyConsumer != null) {
            this.policyConsumer.close();
        }
        super.tearDown();
    }
}