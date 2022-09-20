/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class PCCMockTest extends PCCMockCommon {
    private final String localAddress2 = "127.0.0.2";
    private final String localIpAddress = "127.0.0.1";
    private final String[] mainInput = new String[]{"--local-address", localIpAddress,
        "--remote-address", InetSocketAddressUtil.toHostAndPort(remoteAddress).toString(), "--pcc", "1",
        "--lsp", "3", "--log-level", "DEBUG", "-ka", "10", "-d", "40", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1"};

    @Test
    public void testSessionEstablishment() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, remoteAddress);
        Main.main(mainInput);
        Thread.sleep(1000);
        //3 reported LSPs + syc
        final int numMessages = 4;
        final TestingSessionListener sessionListener = checkSessionListener(numMessages, channel, factory,
                localIpAddress);
        checkSession(sessionListener.getSession(), Uint8.valueOf(40), Uint8.TEN);
    }


    @Test
    public void testMockPCCToManyPCE() throws Exception {
        final InetSocketAddress serverAddress2 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final InetSocketAddress serverAddress3 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
        final InetSocketAddress serverAddress4 = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();

        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final TestingSessionListenerFactory factory2 = new TestingSessionListenerFactory();
        final TestingSessionListenerFactory factory3 = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, serverAddress2);
        final Channel channel2 = createServer(factory2, serverAddress3);
        final Channel channel3 = createServer(factory3, serverAddress4);

        Main.main(new String[]{"--local-address", localIpAddress, "--remote-address",
                InetSocketAddressUtil.toHostAndPort(serverAddress2).toString() + ","
                        + InetSocketAddressUtil.toHostAndPort(serverAddress3).toString() + ","
                        + InetSocketAddressUtil.toHostAndPort(serverAddress4).toString(), "--pcc", "2"});
        Thread.sleep(1000);
        //PCE1
        final int numMessages = 2;
        checkSessionListener(numMessages, channel, factory, localIpAddress);
        checkSessionListener(numMessages, channel, factory, localAddress2);
        //PCE2
        checkSessionListener(numMessages, channel2, factory2, localIpAddress);
        checkSessionListener(numMessages, channel2, factory2, localAddress2);
        //PCE3
        checkSessionListener(numMessages, channel3, factory3, localIpAddress);
        checkSessionListener(numMessages, channel3, factory3, localAddress2);
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        return Collections.emptyList();
    }
}
