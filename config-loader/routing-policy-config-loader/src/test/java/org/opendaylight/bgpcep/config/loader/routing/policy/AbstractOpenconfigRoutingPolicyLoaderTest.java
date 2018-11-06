/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.routing.policy;

import static org.junit.Assert.assertNotNull;
import static org.opendaylight.bgpcep.config.loader.routing.policy.OpenconfigRoutingPolicyLoader.ROUTING_POLICY_IID;
import static org.opendaylight.protocol.util.CheckTestUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckTestUtil.checkPresentConfiguration;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoader;

public class AbstractOpenconfigRoutingPolicyLoaderTest extends AbstractConfigLoader {
    OpenconfigRoutingPolicyLoader policyLoader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        checkNotPresentConfiguration(getDataBroker(), ROUTING_POLICY_IID);
        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/routing-policy-config.xml"));
        this.policyLoader = new OpenconfigRoutingPolicyLoader(this.configLoader, getDataBroker());
        this.policyLoader.init();
        checkPresentConfiguration(getDataBroker(), ROUTING_POLICY_IID);
        this.policyLoader.close();
        this.configLoader.close();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
