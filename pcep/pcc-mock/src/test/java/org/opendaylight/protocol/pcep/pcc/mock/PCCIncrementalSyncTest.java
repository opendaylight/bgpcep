/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.channel.Channel;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.impl.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCServerPeerProposal;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCCIncrementalSyncTest extends PCCMockCommon {
    /**
     * Test Incremental Synchronization
     * Create 8 lsp, then it disconnects after 5 sec and then after 5 sec reconnects with Pcc DBVersion 10
     * After reconnection PCE has DBVersion 10, therefore there is 9 changes missed. 9 Pcrt + 1 Pcrt-Sync
     */
    private final String[] mainInputIncrementalSync = new String[]{"--local-address", localAddress.getHostString(),
        "--remote-address", InetSocketAddressUtil.toHostAndPort(remoteAddress).toString(), "--pcc", "1", "--lsp",
        "8", "--log-level", "DEBUG", "-ka", "30", "-d", "120", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1", "--incremental-sync-procedure", "10", "5", "5"};

    @Test
    public void testSessionIncrementalSyncEstablishment() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Uint64 numberOflspAndDBv = Uint64.valueOf(8);
        final Channel channel = createServer(factory, remoteAddress, new PCCServerPeerProposal(numberOflspAndDBv));
        Main.main(mainInputIncrementalSync);
        final TestingSessionListener pceSessionListener = getListener(factory);
        checkSynchronizedSession(8, pceSessionListener, numberOflspAndDBv);
        Thread.sleep(6000);
        final int expetecdNumberOfLspAndEndOfSync = 3;
        final Uint64 expectedFinalDBVersion = Uint64.TEN;
        final TestingSessionListener sessionListenerAfterReconnect = getListener(factory);
        checkResyncSession(Optional.empty(), expetecdNumberOfLspAndEndOfSync, 3, numberOflspAndDBv,
                expectedFinalDBVersion, sessionListenerAfterReconnect);
        channel.close().get();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        return List.of(new PCEPStatefulCapability(true, true, false, false, true, true));
    }
}
