/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.concepts.KeyMapping;

/**
 * Dispatcher class for creating BGP server.
 */
public interface BGPServerDispatcher {

    /**
     * Create new BGP server to accept incoming bgp connections (bound to provided socket address).
     *
     * @param peerRegistry BGP peer registry
     * @param address server address
     * @return ChannelFuture promising a server session
     */
    ChannelFuture createServer(BGPPeerRegistry peerRegistry, InetSocketAddress address);

    /**
     * Create new BGP server to accept incoming bgp connections (bound to provided socket address).
     *
     * @param peerRegistry BGP peer registry
     * @param address server address
     * @param keys KeyMapping
     * @return ChannelFuture promising a server session
     */
    ChannelFuture createServer(BGPPeerRegistry peerRegistry, InetSocketAddress address, KeyMapping keys);
}
