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
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.netconf.transport.spi.TcpMd5Secrets;

/**
 * Dispatcher class for creating servers and clients.
 */
@NonNullByDefault
public interface PCEPDispatcher {
    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param listenAddress Server listen address
     * @param secrets RFC2385 TCP-MD5 keys
     * @param registry a message registry
     * @param negotiatorFactory a negotiation factory
     * @return A future completing when the PCEP server is created
     */
    ChannelFuture createServer(InetSocketAddress listenAddress, TcpMd5Secrets secrets, MessageRegistry registry,
        PCEPSessionNegotiatorFactory negotiatorFactory);
}
