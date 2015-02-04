/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class TestClientDispatcher extends AbstractDispatcher<BGPSessionImpl, BGPSessionListener> {

    private final EventLoopGroup workerGroup;
    private final BGPHandlerFactory hf;

    protected TestClientDispatcher(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MessageRegistry messageRegistry) {
        super(bossGroup, workerGroup);
        this.workerGroup = workerGroup;
        this.hf = new BGPHandlerFactory(messageRegistry);
    }

    /**
     * Creates a client.
     *
     * @param localAddress local address
     * @param remoteAddress remote address
     * @param connectStrategy Reconnection strategy to be used when initial connection fails
     *
     * @return Future representing the connection process. Its result represents the combined success of TCP connection
     *         as well as session negotiation.
     */
    public Future<BGPSessionImpl> createClient(final InetSocketAddress localAddress, final InetSocketAddress remoteAddress,
            final AsNumber remoteAs, final BGPPeerRegistry listener, final ReconnectStrategy strategy) {
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.localAddress(localAddress).option(ChannelOption.SO_KEEPALIVE, true);
        customizeBootstrap(bootstrap);
        bootstrap.group(this.workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, listener);
        return super.createClient(remoteAddress, strategy, bootstrap, new PipelineInitializer<BGPSessionImpl>() {

            @Override
            public void initializeChannel(SocketChannel ch, Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(TestClientDispatcher.this.hf.getDecoders());
                ch.pipeline().addLast("negotiator", snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(TestClientDispatcher.this.hf.getEncoders());
            }
        });
    }
}
