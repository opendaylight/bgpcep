/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;
import org.opendaylight.protocol.bmp.impl.BmpActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderContext;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMock {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMock.class);

    public static void main(final String[] args) {
        LOG.info("Starting BMP test tool.");
        final BmpMockArguments arguments = BmpMockArguments.parseArguments(args);
        initiateLogger(arguments);
        final BmpMockDispatcher dispatcher = initiateMock(arguments);
        deployClients(dispatcher, arguments);

    }

    private static void initiateLogger(final BmpMockArguments arguments) {
        getRootLogger((LoggerContext) LoggerFactory.getILoggerFactory()).setLevel(arguments.getLogLevel());
    }

    private static BmpMockDispatcher initiateMock(final BmpMockArguments arguments) {
        final BmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        final BmpExtensionProviderActivator bmpActivator = new BmpActivator(
                ServiceLoaderBGPExtensionProviderContext.getSingletonInstance());
        bmpActivator.start(ctx);

        return new BmpMockDispatcher(ctx.getBmpMessageRegistry(),
                new BmpSessionFactory() {
                    @Override
                    public BmpSession getSession(final Channel channel,
                            final BmpSessionListenerFactory sessionListenerFactory) {
                        return new BmpMockSession(arguments.getPeersCount(), arguments.getPrePolicyRoutesCount(), arguments.getPostPolicyRoutesCount());
                    }
                });
    }

    private static void deployClients(final BmpMockDispatcher dispatcher, final BmpMockArguments arguments) {
        final InetSocketAddress localAddress = arguments.getLocalAddress();
        final InetSocketAddress remoteAddress = arguments.getRemoteAddress();
        InetAddress currentLocal = localAddress.getAddress();
        final int port = localAddress.getPort();
        for (int i = 0; i < arguments.getRoutersCount(); i++) {
            dispatcher.createClient(new InetSocketAddress(currentLocal, port), remoteAddress);
            currentLocal = InetAddresses.increment(currentLocal);
        }
    }

    private static ch.qos.logback.classic.Logger getRootLogger(final LoggerContext lc) {
        return Iterables.find(lc.getLoggerList(), new Predicate<Logger>() {
            @Override
            public boolean apply(final Logger input) {
                return (input != null) ? input.getName().equals(Logger.ROOT_LOGGER_NAME) : false;
            }
        });
    }
}
