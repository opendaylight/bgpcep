/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import com.google.common.net.InetAddresses;
import io.netty.channel.ChannelFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.parser.BmpActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.protocol.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMock {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMock.class);

    private BmpMock() {
        throw new UnsupportedOperationException();
    }

    public static void main(final String[] args) {
        deploy(args);
    }


    private static BmpMockDispatcher initiateMock(final BmpMockArguments arguments) {
        final BGPExtensionProviderContext bgpCtx = new SimpleBGPExtensionProviderContext();
        final BGPActivator bgpActivator = new BGPActivator();
        bgpActivator.start(bgpCtx);
        final BmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        final BmpExtensionProviderActivator bmpActivator = new BmpActivator(bgpCtx);
        bmpActivator.start(ctx);

        return new BmpMockDispatcher(ctx.getBmpMessageRegistry(), new BmpMockSessionFactory(arguments));
    }

    private static List<ChannelFuture> deployClients(final BmpMockDispatcher dispatcher,
                                                     final BmpMockArguments arguments) {
        final InetSocketAddress localAddress = arguments.getLocalAddress();
        InetAddress currentLocal = localAddress.getAddress();
        final int port = localAddress.getPort();
        final List<ChannelFuture> futureClients = new ArrayList<>();
        for (int i = 0; i < arguments.getRoutersCount(); i++) {
            for (final InetSocketAddress remoteAddress : arguments.getRemoteAddress()) {
                futureClients.add(dispatcher.createClient(new InetSocketAddress(currentLocal, port), remoteAddress));
            }
            currentLocal = InetAddresses.increment(currentLocal);
        }
        return futureClients;
    }

    private static List<ChannelFuture> deployServers(final BmpMockDispatcher dispatcher,
                                                     final BmpMockArguments arguments) {
        final InetSocketAddress localAddress = arguments.getLocalAddress();
        InetAddress currentLocal = localAddress.getAddress();
        final int port = localAddress.getPort();
        final List<ChannelFuture> futureServers = new ArrayList<>();
        for (int i = 0; i < arguments.getRoutersCount(); i++) {
            futureServers.add(dispatcher.createServer(new InetSocketAddress(currentLocal, port)));
            currentLocal = InetAddresses.increment(currentLocal);
        }
        return futureServers;
    }

    public static List<ChannelFuture> deploy(String [] args) {
        LOG.info("Starting BMP test tool.");
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(args);
        LoggerUtil.initiateLogger(arguments);

        final BmpMockDispatcher dispatcher = initiateMock(arguments);
        // now start the server / client
        if (arguments.isOnPassiveMode()) {
            return deployServers(dispatcher, arguments);
        } else {
            return deployClients(dispatcher, arguments);
        }
    }

}
