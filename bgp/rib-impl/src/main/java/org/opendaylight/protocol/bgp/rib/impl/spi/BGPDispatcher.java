/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.base.Optional;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * Dispatcher class for creating BGP clients.
 */
public interface BGPDispatcher extends BGPServerDispatcher {

    /**
     * Creates BGP client.
     *
     * @param address Peer address
     * @param remoteAs remote AS
     * @param peerRegistry BGP peer registry
     * @param strategy reconnection strategy
     * @return Future promising a client session
     */
    Future<? extends BGPSession> createClient(InetSocketAddress address, AsNumber remoteAs,
            BGPPeerRegistry peerRegistry, ReconnectStrategy strategy);

    Future<Void> createReconnectingClient(InetSocketAddress address, AsNumber remoteAs,
                                          BGPPeerRegistry peerRegistry, ReconnectStrategyFactory connectStrategyFactory,
            ReconnectStrategyFactory reestablishStrategyFactory);

    Future<Void> createReconnectingClient(InetSocketAddress address, AsNumber remoteAs,
                                          BGPPeerRegistry peerRegistry, ReconnectStrategyFactory connectStrategyFactory,
            ReconnectStrategyFactory reestablishStrategyFactory, Optional<KeyMapping> keys);
}
