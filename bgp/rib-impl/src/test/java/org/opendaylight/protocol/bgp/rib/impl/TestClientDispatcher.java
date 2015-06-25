/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class TestClientDispatcher {

    private final BGPHandlerFactory hf;
    private final InetSocketAddress defaulAddress;
    private InetSocketAddress localAddress;
    private final BGPDispatcherImpl disp;

    protected TestClientDispatcher(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MessageRegistry messageRegistry,
                                   final InetSocketAddress locaAddress) {
        disp = new BGPDispatcherImpl(messageRegistry, bossGroup, workerGroup) {
            @Override
            protected void customizeBootstrap(final Bootstrap b) {
                b.localAddress(locaAddress);
            }
        };
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.localAddress = locaAddress;
        this.defaulAddress = locaAddress;
    }

    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress remoteAddress,
                                                            final AsNumber remoteAs, final BGPPeerRegistry listener, final ReconnectStrategy strategy, final Optional<InetSocketAddress> localAddress) {
        setLocalAddress(localAddress);
        return disp.createClient(remoteAddress, remoteAs, listener, strategy);
    }

    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address,
                                                              final AsNumber remoteAs, final BGPPeerRegistry peerRegistry, final ReconnectStrategyFactory reconnectStrategyFactory,
                                                              final Optional<InetSocketAddress> localAddress) {
        setLocalAddress(localAddress);
        return disp.createReconnectingClient(address, remoteAs, peerRegistry, reconnectStrategyFactory, null);
    }

    private synchronized void setLocalAddress(final Optional<InetSocketAddress> localAddress) {
        if (localAddress.isPresent()) {
            this.localAddress = localAddress.get();
        } else {
            this.localAddress = defaulAddress;
        }
    }
}
