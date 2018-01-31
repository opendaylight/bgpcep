/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.impl;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.TagType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

public class RoutingPolicyProviderTest extends AbstractRoutingPolicyProviderTest {
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testRoutingPolicyProvider() {
        assertNotNull(this.policyConsumer.getPolicy("default-odl-import-policy"));
        assertTrue(this.policyConsumer.matchPrefix("prefix-set",
                new IpPrefix(new Ipv4Prefix("192.168.0.0/16"))));
        assertFalse(this.policyConsumer.matchPrefix("prefix-set",
                new IpPrefix(new Ipv4Prefix("192.0.0.0/16"))));
        assertTrue(this.policyConsumer.matchNeighbor("neighbor-set",
                new IpAddress(new Ipv4Address("123.42.13.8"))));
        assertFalse(this.policyConsumer.matchNeighbor("neighbor-set",
                new IpAddress(new Ipv4Address("123.42.13.7"))));
        assertTrue(this.policyConsumer.matchTag("tag-name", new TagType(2L)));
        assertFalse(this.policyConsumer.matchTag("tag-name-fail", new TagType(3L)));
    }
}