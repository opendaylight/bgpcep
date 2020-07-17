/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertNotNull;

import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.ietf.stateful.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCPeerProposal;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCCSyncAvoidanceProcedureTest extends PCCMockCommon {
    @Test
    public void testSessionAvoidanceDesynchronizedEstablishment() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();

        final Channel channel = createServer(factory, this.remoteAddress, new PCCPeerProposal());
        final PCEPSession session = createPCCSession(Uint64.TEN).get();
        assertNotNull(session);
        final TestingSessionListener pceSessionListener = getListener(factory);
        assertNotNull(pceSessionListener);
        assertNotNull(pceSessionListener.getSession());
        checkResyncSession(Optional.empty(), 11, 11, null, Uint64.TEN, pceSessionListener);
        channel.close().get();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, true, true, false,
            false, false, true));
        return caps;
    }
}
