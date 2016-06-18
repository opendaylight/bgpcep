/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.base.Optional;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.concepts.KeyMapping;

/**
 * Dispatcher class for creating BGP clients.
 */
public interface BGPDispatcher{

    /**
     * Creates BGP client.
     *
     * @param remoteAddress remote Peer address
     * @param peerRegistry BGP peer registry
     * @param retryTimer Retry timer
     * @return Future promising a client session
     */
    Future<? extends BGPSession> createClient(InetSocketAddress remoteAddress, BGPPeerRegistry peerRegistry, int retryTimer);

    /**
     * Creates Reconnecting client.
     *
     * @param remoteAddress remote Peer Address
     * @param peerRegistry BGP peer registry
     * @param retryTimer Retry timer
     * @param keys for TCPMD5
     * @return Future promising a client session
     */
    Future<Void> createReconnectingClient(InetSocketAddress remoteAddress,
        BGPPeerRegistry peerRegistry, int retryTimer, Optional<KeyMapping> keys);

    /**
     * Create new BGP server to accept incoming bgp connections (bound to provided socket localAddress).
     *
     * @param peerRegistry BGP peer registry
     * @param localAddress Peer localAddress
     *
     * @return ChannelFuture promising a client session
     */
    ChannelFuture createServer(BGPPeerRegistry peerRegistry, InetSocketAddress localAddress);
}
