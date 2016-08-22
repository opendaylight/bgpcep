/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

@RunWith(Parameterized.class)
public final class PCCMockTest extends PCCMockCommon {
    private final String[] mainInput = new String[] {"--local-address", this.localAddress.getHostString(), "--remote-address",
        InetSocketAddressUtil.toHostAndPort(this.remoteAddress).toString(), "--pcc", "1", "--lsp", "3", "--log-level", "DEBUG", "-ka", "10", "-d", "40", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1"};

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[10][0]);
    }
    @Test
    public void testSessionEstablishment() throws Exception {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, this.remoteAddress);
        Main.main(mainInput);
        Thread.sleep(1000);
        //3 reported LSPs + syc
        final int numMessages = 4;
        final TestingSessionListener sessionListener = checkSessionListener(numMessages, channel, factory, this.localAddress.getHostString());
        checkSession(sessionListener.getSession(), 40, 10);
        sessionListener.getSession().close();
        channel.close();
    }

    @Test
    public void testMockPCCToManyPCE() throws Exception {
        final int SERVER_COUNT = 2;
        final int CLIENT_COUNT = 1;
        final String[] localAddress = new String[CLIENT_COUNT];
        final String[] serverAddress = new String[SERVER_COUNT];
        final TestingSessionListenerFactory[] factory = new TestingSessionListenerFactory[SERVER_COUNT];
        final Channel[] channel = new Channel[SERVER_COUNT];
        for (int i = 0; i < SERVER_COUNT; i++) {
            final InetSocketAddress randSrvAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress();
            serverAddress[i] = InetSocketAddressUtil.toHostAndPort(randSrvAddr).toString();
            factory[i] = new TestingSessionListenerFactory();
            channel[i] = createServer(factory[i], randSrvAddr);
        }
        localAddress[0] = InetSocketAddressUtil.getRandomLoopbackIpAddress();
        for (int i = 1; i < CLIENT_COUNT; i++) {
            localAddress[i] = InetAddresses.increment(InetAddresses.forString(localAddress[i - 1])).getHostAddress();
        }

        Main.main(new String[] {"--local-address", localAddress[0], "--remote-address",
            String.join(",", Arrays.asList(serverAddress)),
            "--pcc", new Integer(CLIENT_COUNT).toString()});
        Thread.sleep(1000);

        final int NUM_OF_MSG = 2;
        for (int i = 0; i < SERVER_COUNT; i++) {
            for (int j = 0; j < CLIENT_COUNT; j++) {
                checkSessionListener(NUM_OF_MSG, channel[i], factory[i], localAddress[j]);
            }
            channel[i].close().get();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<MsgBuilderUtil> c = MsgBuilderUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Override
    protected List<PCEPCapability> getCapabilities() {
        return Collections.emptyList();
    }
}
