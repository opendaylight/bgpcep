/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * Dispatcher class for creating BGP clients.
 */
public interface BGPDispatcher {

    /**
     * Creates BGP client.
     *
     * @param address Peer address
     * @param preferences connection attributes required for connection
     * @param listener BGP message listener
     * @return Future promising a client session
     */
    Future<? extends BGPSession> createClient(InetSocketAddress address, BGPSessionPreferences preferences, AsNumber remoteAs,
            BGPSessionListener listener, ReconnectStrategy strategy);

    Future<Void> createReconnectingClient(InetSocketAddress address, BGPSessionPreferences preferences, AsNumber remoteAs,
            BGPSessionListener listener, ReconnectStrategyFactory connectStrategyFactory,
            ReconnectStrategyFactory reestablishStrategyFactory);

    Future<Void> createReconnectingClient(InetSocketAddress address, BGPSessionPreferences preferences, AsNumber remoteAs,
            BGPSessionListener listener, ReconnectStrategyFactory connectStrategyFactory,
            ReconnectStrategyFactory reestablishStrategyFactory, KeyMapping keys);
}
