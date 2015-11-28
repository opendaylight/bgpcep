/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.channel.Channel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPCapability;

public class PCCMockTest extends PCCMockCommon {
    private static final String REMOTE_ADDRESS2 = "127.0.2.0";
    private static final String REMOTE_ADDRESS3 = "127.0.3.0";
    private static final String REMOTE_ADDRESS4 = "127.0.4.0";
    private static final String LOCAL_ADDRESS2 = "127.0.0.2";
    private static final InetSocketAddress SERVER_ADDRESS2 = new InetSocketAddress(REMOTE_ADDRESS2, 4189);
    private static final InetSocketAddress SERVER_ADDRESS3 = new InetSocketAddress(REMOTE_ADDRESS3, 4189);
    private static final InetSocketAddress SERVER_ADDRESS4 = new InetSocketAddress(REMOTE_ADDRESS4, 4189);
    private final String[] mainInput = new String[]{"--local-address", PCCMockCommon.LOCAL_ADDRESS, "--remote-address",
        PCCMockCommon.REMOTE_ADDRESS + ":4567", "--pcc", "1", "--lsp", "3", "--log-level", "DEBUG", "-ka", "10", "-d", "40", "--reconnect", "-1",
        "--redelegation-timeout", "0", "--state-timeout", "-1"};

    @Test
    public void testSessionEstablishment() throws UnknownHostException, InterruptedException, ExecutionException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, this.socket);
        Main.main(mainInput);
        Thread.sleep(1000);
        //3 reported LSPs + syc
        final int numMessages = 4;
        final TestingSessionListener sessionListener = checkSessionListener(numMessages, channel, factory, PCCMockCommon.LOCAL_ADDRESS);
        checkSession(sessionListener.getSession(), 40, 10);
    }


    @Test
    public void testMockPCCToManyPCE() throws InterruptedException, ExecutionException, UnknownHostException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final TestingSessionListenerFactory factory2 = new TestingSessionListenerFactory();
        final TestingSessionListenerFactory factory3 = new TestingSessionListenerFactory();
        final Channel channel = createServer(factory, SERVER_ADDRESS2);
        final Channel channel2 = createServer(factory2, SERVER_ADDRESS3);
        final Channel channel3 = createServer(factory3, SERVER_ADDRESS4);

        Main.main(new String[]{"--local-address", PCCMockCommon.LOCAL_ADDRESS, "--remote-address", REMOTE_ADDRESS2 + "," + REMOTE_ADDRESS3 + "," + REMOTE_ADDRESS4, "--pcc", "2"});
        Thread.sleep(1000);
        //PCE1
        int numMessages = 2;
        checkSessionListener(numMessages, channel, factory, PCCMockCommon.LOCAL_ADDRESS);
        checkSessionListener(numMessages, channel, factory, LOCAL_ADDRESS2);
        //PCE2
        checkSessionListener(numMessages, channel2, factory2, LOCAL_ADDRESS);
        checkSessionListener(numMessages, channel2, factory2, LOCAL_ADDRESS2);
        //PCE3
        checkSessionListener(numMessages, channel3, factory3, PCCMockCommon.LOCAL_ADDRESS);
        checkSessionListener(numMessages, channel3, factory3, LOCAL_ADDRESS2);
    }

    @Test(expected=UnsupportedOperationException.class)
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
