/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.BmpNettyGroups;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.util.InetSocketAddressUtil;

@ExtendWith(MockitoExtension.class)
class BmpMockDispatcherTest {
    private final BmpSessionFactory sessionFactory = new DefaultBmpSessionFactory();
    private final BmpMockSessionListener sl = new BmpMockSessionListener();
    @Mock
    private BmpMessageRegistry registry;
    @Mock
    private BmpSessionListenerFactory slf;
    @Mock
    private BmpExtensionProviderContext ctx;

    private BmpMockDispatcher bmpMockDispatcher;

    @BeforeEach
    void setUp() {
        doReturn(sl).when(slf).getSessionListener();
        doReturn(registry).when(ctx).getBmpMessageRegistry();
        bmpMockDispatcher = new BmpMockDispatcher(registry, sessionFactory);
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testCreateClient() throws Exception {
        final int port = InetSocketAddressUtil.getRandomPort();
        final InetSocketAddress serverAddr = InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port);

        var threadGroups = new BmpNettyGroups();
        final BmpDispatcherImpl bmpDispatcher = new BmpDispatcherImpl(threadGroups, ctx, sessionFactory);
        final ChannelFuture futureServer = bmpDispatcher
                .createServer(serverAddr, slf, KeyMapping.of());
        waitFutureSuccess(futureServer);

        final ChannelFuture channelFuture = bmpMockDispatcher.createClient(InetSocketAddressUtil
                .getRandomLoopbackInetSocketAddress(0), serverAddr);
        final Channel channel = channelFuture.sync().channel();

        assertTrue(channel.isActive());
        checkEquals(() -> assertTrue(sl.getStatus()));
        channel.close();
        bmpDispatcher.close();
        threadGroups.close();
        checkEquals(() -> assertFalse(sl.getStatus()));

        threadGroups = new BmpNettyGroups();
        final BmpDispatcherImpl bmpDispatcher2 = new BmpDispatcherImpl(threadGroups, ctx, sessionFactory);
        final ChannelFuture futureServer2 = bmpDispatcher2.createServer(serverAddr, slf, KeyMapping.of());
        futureServer2.sync();
        checkEquals(() -> assertTrue(sl.getStatus()));

        bmpDispatcher2.close();
        threadGroups.close();
        bmpMockDispatcher.close();
        checkEquals(() -> assertFalse(sl.getStatus()));
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testCreateServer() throws Exception {
        final int port = InetSocketAddressUtil.getRandomPort();
        final BmpNettyGroups threadGroups = new BmpNettyGroups();
        final BmpDispatcherImpl bmpDispatcher = new BmpDispatcherImpl(threadGroups, ctx, sessionFactory);
        final ChannelFuture futureServer = bmpMockDispatcher.createServer(
                new InetSocketAddress(InetAddresses.forString("0.0.0.0"), port));
        futureServer.sync();
        final ChannelFuture channelFuture = bmpDispatcher.createClient(
                InetSocketAddressUtil.getRandomLoopbackInetSocketAddress(port), slf, KeyMapping.of());
        final Channel channel = channelFuture.sync().channel();

        assertTrue(channel.isActive());
        checkEquals(() -> assertTrue(sl.getStatus()));
        assertTrue(futureServer.channel().isActive());
        channel.close();

        bmpDispatcher.close();
        threadGroups.close();
        bmpMockDispatcher.close();
        checkEquals(() -> assertFalse(sl.getStatus()));
    }
}
