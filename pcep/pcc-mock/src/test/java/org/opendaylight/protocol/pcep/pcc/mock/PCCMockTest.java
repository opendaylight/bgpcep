/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Charsets;
import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPDispatcherImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.jni.NativeKeyAccessFactory;
import org.opendaylight.tcpmd5.jni.NativeSupportUnavailableException;
import org.opendaylight.tcpmd5.jni.NativeTestSupport;
import org.opendaylight.tcpmd5.netty.MD5NioServerSocketChannelFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;

public class PCCMockTest {

    private static final short KEEP_ALIVE = 30;
    private static final short DEAD_TIMER = 120;
    private static final String REMOTE_ADDRESS = "127.0.1.0";
    private static final String REMOTE_ADDRESS2 = "127.0.2.0";
    private static final String REMOTE_ADDRESS3 = "127.0.3.0";
    private static final String REMOTE_ADDRESS4 = "127.0.4.0";
    private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(REMOTE_ADDRESS, 4189);
    private static final InetSocketAddress SERVER_ADDRESS2 = new InetSocketAddress(REMOTE_ADDRESS2, 4189);
    private static final InetSocketAddress SERVER_ADDRESS3 = new InetSocketAddress(REMOTE_ADDRESS3, 4189);
    private static final InetSocketAddress SERVER_ADDRESS4 = new InetSocketAddress(REMOTE_ADDRESS4, 4189);
    private static final String LOCAL_ADDRESS = "127.0.0.1";
    private static final String LOCAL_ADDRESS2 = "127.0.0.2";

    private PCEPDispatcher pceDispatcher;

    @Before
    public void setUp() {
        final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(
                new OpenBuilder().setKeepalive(KEEP_ALIVE).setDeadTimer(DEAD_TIMER).setSessionId((short) 0).build(), 0);
        this.pceDispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
                nf, new NioEventLoopGroup(), new NioEventLoopGroup());
    }

    @Test
    public void testSessionEstablishment() throws UnknownHostException, InterruptedException, ExecutionException {
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final Channel channel = this.pceDispatcher.createServer(SERVER_ADDRESS, factory).channel();
        Main.main(new String[] {"--local-address", LOCAL_ADDRESS, "--remote-address", REMOTE_ADDRESS, "--pcc", "1", "--lsp", "3",
            "--log-level", "DEBUG", "-ka", "10", "-d", "40"});
        Thread.sleep(1000);
        final TestingSessionListener sessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS));
        Assert.assertTrue(sessionListener.isUp());
        //3 reported LSPs + syc
        Assert.assertEquals(4, sessionListener.messages().size());
        final PCEPSession session = sessionListener.getSession();
        Assert.assertNotNull(session);
        Assert.assertEquals(40, session.getPeerPref().getDeadtimer().shortValue());
        Assert.assertEquals(10, session.getPeerPref().getKeepalive().shortValue());
        channel.close().get();
    }

    @Test
    public void testMockPCCToManyPCE() throws InterruptedException, ExecutionException, UnknownHostException {
        final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(
                new OpenBuilder().setKeepalive(KEEP_ALIVE).setDeadTimer(DEAD_TIMER).setSessionId((short) 0).build(), 0);
        PCEPDispatcher dispatcher2 = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
                nf, new NioEventLoopGroup(), new NioEventLoopGroup());
        final DefaultPCEPSessionNegotiatorFactory nf2 = new DefaultPCEPSessionNegotiatorFactory(
                new OpenBuilder().setKeepalive(KEEP_ALIVE).setDeadTimer(DEAD_TIMER).setSessionId((short) 0).build(), 0);
        PCEPDispatcher dispatcher3 = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
                nf2, new NioEventLoopGroup(), new NioEventLoopGroup());
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final TestingSessionListenerFactory factory2 = new TestingSessionListenerFactory();
        final TestingSessionListenerFactory factory3 = new TestingSessionListenerFactory();
        final Channel channel = pceDispatcher.createServer(SERVER_ADDRESS2, factory).channel();
        final Channel channel2 = dispatcher2.createServer(SERVER_ADDRESS3, factory2).channel();
        final Channel channel3 = dispatcher3.createServer(SERVER_ADDRESS4, factory3).channel();

        Main.main(new String[] {"--local-address", LOCAL_ADDRESS, "--remote-address", REMOTE_ADDRESS2 + "," + REMOTE_ADDRESS3 + "," + REMOTE_ADDRESS4,
            "--pcc", "2"});
        Thread.sleep(1000);
        //PCE1
        TestingSessionListener sessionListener1 = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS));
        TestingSessionListener sessionListener2 = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS2));
        Assert.assertNotNull(sessionListener1);
        Assert.assertNotNull(sessionListener2);
        Assert.assertTrue(sessionListener1.isUp());
        Assert.assertTrue(sessionListener2.isUp());
        Assert.assertEquals(2, sessionListener1.messages().size());
        Assert.assertEquals(2, sessionListener2.messages().size());
        //PCE2
        sessionListener1 = factory2.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS));
        sessionListener2 = factory2.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS2));
        Assert.assertNotNull(sessionListener1);
        Assert.assertNotNull(sessionListener2);
        Assert.assertTrue(sessionListener1.isUp());
        Assert.assertTrue(sessionListener2.isUp());
        Assert.assertEquals(2, sessionListener1.messages().size());
        Assert.assertEquals(2, sessionListener2.messages().size());
        //PCE3
        sessionListener1 = factory3.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS));
        sessionListener2 = factory3.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS2));
        Assert.assertNotNull(sessionListener1);
        Assert.assertNotNull(sessionListener2);
        Assert.assertTrue(sessionListener1.isUp());
        Assert.assertTrue(sessionListener2.isUp());
        Assert.assertEquals(2, sessionListener1.messages().size());
        Assert.assertEquals(2, sessionListener2.messages().size());

        channel.close().get();
        channel2.close().get();
        channel3.close().get();
    }

    @Test
    public void testMockPccWithMD5() throws NativeSupportUnavailableException, InterruptedException, ExecutionException, UnknownHostException {
        NativeTestSupport.assumeSupportedPlatform();
        final DefaultPCEPSessionNegotiatorFactory nf = new DefaultPCEPSessionNegotiatorFactory(
                new OpenBuilder().setKeepalive(KEEP_ALIVE).setDeadTimer(DEAD_TIMER).setSessionId((short) 0).build(), 0);
        final PCEPDispatcher dispatcher = new PCEPDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry(),
                nf, new NioEventLoopGroup(), new NioEventLoopGroup(), null, new MD5NioServerSocketChannelFactory(NativeKeyAccessFactory.getInstance()));
        final TestingSessionListenerFactory factory = new TestingSessionListenerFactory();
        final KeyMapping keyMapping = new KeyMapping();
        keyMapping.put(InetAddresses.forString(LOCAL_ADDRESS), "pazzword".getBytes(Charsets.US_ASCII));
        final Channel channel = dispatcher.createServer(SERVER_ADDRESS, keyMapping, factory).channel();
        Main.main(new String[] {"--local-address", LOCAL_ADDRESS, "--remote-address", REMOTE_ADDRESS, "--pcc", "1", "--lsp", "3", "--password", "pazzword"});
        Thread.sleep(1000);
        final TestingSessionListener sessionListener = factory.getSessionListenerByRemoteAddress(InetAddresses.forString(LOCAL_ADDRESS));
        Assert.assertTrue(sessionListener.isUp());
        channel.close().get();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<MsgBuilderUtil> c = MsgBuilderUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
