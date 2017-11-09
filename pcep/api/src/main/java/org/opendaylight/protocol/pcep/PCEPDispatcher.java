/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.opendaylight.protocol.concepts.KeyMapping;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface PCEPDispatcher {
    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param address to be bound with the server
     * @param listenerFactory to create listeners for clients
     * @param peerProposal information used in our Open message
     * @return instance of PCEPServer
     */
    ChannelFuture createServer(InetSocketAddress address, PCEPSessionListenerFactory listenerFactory,
        PCEPPeerProposal peerProposal);

    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param address to be bound with the server
     * @param keys RFC2385 key mapping
     * @param listenerFactory to create listeners for clients
     * @param peerProposal information used in our Open message
     * @return instance of PCEPServer
     */
    ChannelFuture createServer(InetSocketAddress address, KeyMapping keys, PCEPSessionListenerFactory listenerFactory,
        PCEPPeerProposal peerProposal);

    PCEPSessionNegotiatorFactory<?> getPCEPSessionNegotiatorFactory();
}
