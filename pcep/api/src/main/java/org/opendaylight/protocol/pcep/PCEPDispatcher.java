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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface PCEPDispatcher {
    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param listenAddress Server listen address
     * @param tcpKeys RFC2385 TCP-MD5 keys
     * @param negotiatorDependencies PCEPSessionNegotiatorFactoryDependencies
     * @return A future completing when the PCEP server is created
     */
    @NonNull ChannelFuture createServer(@NonNull InetSocketAddress listenAddress, @NonNull KeyMapping tcpKeys,
        @NonNull PCEPSessionNegotiatorFactoryDependencies negotiatorDependencies);

    @NonNull PCEPSessionNegotiatorFactory getPCEPSessionNegotiatorFactory();
}
