/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.routing.policy;

import static org.junit.Assert.assertNotNull;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import org.junit.Before;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoaderTest;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.OpenconfigRoutingPolicyData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractOpenconfigRoutingPolicyLoaderTest extends AbstractConfigLoaderTest {
    private static final InstanceIdentifier<RoutingPolicy> ROUTING_POLICY_IID =
        InstanceIdentifier.builderOfInherited(OpenconfigRoutingPolicyData.class, RoutingPolicy.class).build();

    OpenconfigRoutingConfigFileProcessor policyLoader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        checkNotPresentConfiguration(getDataBroker(), ROUTING_POLICY_IID);
        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/routing-policy-config.xml"));
        this.policyLoader = new OpenconfigRoutingConfigFileProcessor(this.configLoader, getDomBroker());
        this.policyLoader.init();
        checkPresentConfiguration(getDataBroker(), ROUTING_POLICY_IID);
        this.policyLoader.close();
    }
}
