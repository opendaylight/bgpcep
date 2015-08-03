/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.api;

import com.google.common.base.Optional;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface BmpDispatcher extends AutoCloseable {

    /**
     * Creates client.
     *
     * @param address to connect to
     * @param rcsf    reconnect strategy factory
     * @param keys    RFC2385 key mapping
     * @return instance of BmpClient
     */
    ChannelFuture createReconnectClient(InetSocketAddress address, ReconnectStrategyFactory rcsf, Optional<KeyMapping> keys);
     /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param address to be bound with the server
     * @param slf     Bmp session listener factory
     * @param keys    RFC2385 key mapping
     * @return instance of BmpServer
     */
    ChannelFuture createServer(InetSocketAddress address, BmpSessionListenerFactory slf, Optional<KeyMapping> keys);

}
