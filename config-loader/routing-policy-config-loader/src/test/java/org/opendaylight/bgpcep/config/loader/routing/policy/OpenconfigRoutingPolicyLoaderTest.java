/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.routing.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.bgpcep.config.loader.routing.policy.OpenconfigRoutingPolicyLoader.POLICY_DEFINITIONS_IID;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.routing.policy.PolicyDefinitions;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class OpenconfigRoutingPolicyLoaderTest extends AbstractConfigLoader {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void configFileTest() throws Exception {
        checkNotPresentConfiguration(getDataBroker(), POLICY_DEFINITIONS_IID);

        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/policy-definitions-config.xml"));
        final OpenconfigRoutingPolicyLoader processor = new OpenconfigRoutingPolicyLoader(this.configLoader,
                getDataBroker());
        processor.init();
        checkPresentConfiguration(getDataBroker(), POLICY_DEFINITIONS_IID);

        assertEquals(SchemaPath.create(true, RoutingPolicy.QNAME, PolicyDefinitions.QNAME),
                processor.getSchemaPath());
        processor.close();
    }
}