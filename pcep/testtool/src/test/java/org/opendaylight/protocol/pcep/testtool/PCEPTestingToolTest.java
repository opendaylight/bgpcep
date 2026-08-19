/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.keepalive.message.KeepaliveMessageBuilder;

class PCEPTestingToolTest {

    @Test
    void testSessionEstablishment() throws Exception {
        final String serverAddr = InetSocketAddressUtil
            .toHostAndPort(InetSocketAddressUtil.getRandomLoopbackInetSocketAddress()).toString();
        Main.main(new String[] {"-a", serverAddr,
            "-ka", "10", "-d", "0", "--stateful", "--active", "--instant"});
        PCCMock.main(new String[] {serverAddr});
    }

    @Test
    void testSimpleSessionListener() {
        final TestingSessionListener ssl = new TestingSessionListener();
        assertEquals(0, ssl.messages().size());
        ssl.onMessage(null, new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build());
        assertEquals(1, ssl.messages().size());
        assertInstanceOf(KeepaliveMessage.class, ssl.messages().get(0));
        assertFalse(ssl.isUp());
        ssl.onSessionUp(null);
        assertTrue(ssl.isUp());
        ssl.onSessionDown(null, null);
        assertFalse(ssl.isUp());
    }
}
