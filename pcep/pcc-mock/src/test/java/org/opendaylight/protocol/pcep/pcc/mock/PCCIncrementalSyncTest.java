/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCServerPeerProposal;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

public class PCCIncrementalSyncTest extends PCCMockCommon {

    private final BigInteger lsp = BigInteger.valueOf(8);
    /**
     * Test Incremental Synchronization
     * Create 8 lsp, then it disconnects after 5 sec and then after 5 sec reconnects with Pcc DBVersion 10
     * After reconnection PCE has DBVersion 10, therefore there is 9 changes missed. 9 Pcrt + 1 Pcrt-Sync
     */
    private final String[] mainInputIncrementalSync = new String[]{"--local-address", this.localAddress.getHostString(),
        "--remote-address", InetSocketAddressUtil.toHostAndPort(this.remoteAddress).toString(), "--pcc", "1", "--lsp",
        this.lsp.toString(), "--log-level", "DEBUG", "-ka", "30", "-d", "120", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1", "--incremental-sync-procedure", "10", "5", "5"};

    @Test
    public void testSessionIncrementalSyncEstablishment() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final BigInteger numberOflspAndDBv = BigInteger.valueOf(8);
        final Channel channel = createServer(factory, this.remoteAddress, new PCCServerPeerProposal(numberOflspAndDBv));
        Main.main(this.mainInputIncrementalSync);
        final TestingSessionListener pceSessionListener = getListener(factory);
        checkSynchronizedSession(8, pceSessionListener, numberOflspAndDBv);
        Thread.sleep(6000);
        final int expetecdNumberOfLspAndEndOfSync = 3;
        final BigInteger expectedFinalDBVersion = BigInteger.valueOf(10);
        final TestingSessionListener sessionListenerAfterReconnect = getListener(factory);
        checkResyncSession(Optional.absent(), expetecdNumberOfLspAndEndOfSync, 3, numberOflspAndDBv,
                expectedFinalDBVersion, sessionListenerAfterReconnect);
        channel.close().get();
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        final List<PCEPCapability> caps = new ArrayList<>();
        caps.add(new PCEPStatefulCapability(true, true, true, false, false, true, true));
        return caps;
    }
}
