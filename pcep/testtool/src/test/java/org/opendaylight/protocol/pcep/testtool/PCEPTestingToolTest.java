/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;

public class PCEPTestingToolTest {

    @Test
    public void testSimpleSessionListener() {
        final TestingSessionListener ssl = new TestingSessionListener();
        assertEquals(0, ssl.messages().size());
        ssl.onMessage(null, new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build());
        assertEquals(1, ssl.messages().size());
        assertTrue(ssl.messages().get(0) instanceof KeepaliveMessage);
        assertFalse(ssl.up);
        ssl.onSessionUp(null);
        assertTrue(ssl.up);
        ssl.onSessionDown(null, null);
        assertFalse(ssl.up);
    }
}
