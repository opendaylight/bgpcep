/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.api;

import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.concepts.KeyMapping;

/**
 * Dispatcher class for creating servers and clients.
 */
@NonNullByDefault
public interface BmpDispatcher {
    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param address to be bound with the server
     * @param slf     bmp session listener factory
     * @param keys    RFC2385 key mapping
     * @return instance of BmpServer
     */
    ChannelFuture createServer(InetSocketAddress address, BmpSessionListenerFactory slf, KeyMapping keys);

    /**
     * Creates reconnect clients. Make connection to all active monitored-routers.
     *
     * @param address bmp client to connect to
     * @param slf     bmp session listener factory
     * @param keys    RFC2385 key mapping
     * @return void
     */
    ChannelFuture createClient(InetSocketAddress address, BmpSessionListenerFactory slf, KeyMapping keys);
}
