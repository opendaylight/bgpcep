/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;

public class PCCSyncAvoidanceDesyncTest extends PCCMockCommon {

    private final String[] mainInputSyncAvoidanceDesynchronized = new String[]{"--local-address", PCCMockTest.LOCAL_ADDRESS, "--remote-address",
        PCCMockTest.REMOTE_ADDRESS + ":4567", "--pcc", "1", "--lsp", "3", "--log-level", "DEBUG", "-ka", "10", "-d", "40", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1", "--state-sync-avoidance", "10", "5", "5"};

    @Test
    public void testSessionAvoidanceDesynchronizedEstablishment() throws UnknownHostException, InterruptedException, ExecutionException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, socket, new PCCPeerProposal());
        Main.main(mainInputSyncAvoidanceDesynchronized);
        Thread.sleep(2000);
        final TestingSessionListener pceSessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS));
        checkSynchronizedSession(3, pceSessionListener);
        Thread.sleep(4000);
        assertFalse(pceSessionListener.isUp());
        Thread.sleep(6000);
        Thread.sleep(1000);
        final Long expectedDBVersion = Long.valueOf(10);
        final int expetedNumberOfLsp = 4;
        checkResyncSession(expetedNumberOfLsp, expectedDBVersion, factory.getSessionListenerByRemoteAddress(InetAddresses.forString(PCCMockTest.LOCAL_ADDRESS)));
        channel.close().get();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, false, false, false, false, false, true));
        return caps;
    }

    @Override
    protected int getPort() {
        return 4567;
    }
}
