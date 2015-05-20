/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.api;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface BmpDispatcher {
    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param address to be bound with the server
     * @param listenerFactory to create listeners for clients
     * @return instance of BmpServer
     */
    ChannelFuture createServer(InetSocketAddress address, SessionListenerFactory<BmpSessionListener> slf);

    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param address to be bound with the server
     * @param keys RFC2385 key mapping
     * @return instance of BmpServer
     */
    ChannelFuture createServer(InetSocketAddress address, KeyMapping keys, SessionListenerFactory<BmpSessionListener> slf);

    Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory,
            ReconnectStrategyFactory reestablishStrategyFactory);

    Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory,
            ReconnectStrategyFactory reestablishStrategyFactory, KeyMapping keys);
}
