/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.routing.policy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.routing.policy.top.RoutingPolicy;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class OpenconfigRoutingPolicyLoaderTest extends AbstractOpenconfigRoutingPolicyLoaderTest {
    @Test
    public void configFileTest() {
        assertEquals(SchemaPath.create(true, RoutingPolicy.QNAME), this.policyLoader.getSchemaPath());
    }
}