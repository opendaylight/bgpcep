/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertNotNull;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCPeerProposal;

public class PCCSyncAvoidanceDesyncTest extends PCCMockCommon {
    @Test
    public void testSessionAvoidanceDesynchronizedEstablishment() throws UnknownHostException, InterruptedException, ExecutionException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, socket, new PCCPeerProposal());
        Thread.sleep(200);
        PCEPSession session = createPCCSession(BigInteger.TEN).get();
        assertNotNull(session);
        final TestingSessionListener pceSessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS));
        assertNotNull(pceSessionListener);
        Thread.sleep(1000);
        checkResyncSession(Optional.<Integer>absent(), 11, Long.valueOf(10), pceSessionListener);
        channel.close().get();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, true, true, false, false, false, true));
        return caps;
    }

    @Override
    protected int getPort() {
        return 4567;
    }
}
