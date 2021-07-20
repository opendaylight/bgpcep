/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv6InterfaceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

public class LinkstateGraphBuilderTest {
    @Test
    public void testIpv6ToKey() {
        assertEquals(Uint64.valueOf(0x08090A0B0C0D0E0FL),
            LinkstateGraphBuilder.ipv6ToKey(new Ipv6InterfaceIdentifier("0001:0203:0405:0607:0809:0A0B:0C0D:0E0F")));
    }
}
