/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListener;

/**
 * BindableDispatcher abstract class for creating clients that are bound to a specified local InetSocketAddress.
 */
public abstract class BindableDispatcher<S extends ProtocolSession<?>, L extends SessionListener<?, ?, ?>> extends AbstractDispatcher<S, L> {

    private final EventLoopGroup workerGroup;

    protected BindableDispatcher(final EventExecutor executor, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        super(executor, bossGroup, workerGroup);
        this.workerGroup = workerGroup;
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
    protected Future<S> createClient(final InetSocketAddress localAddress, final InetSocketAddress remoteAddress,
                final ReconnectStrategy strategy, final PipelineInitializer<S> initializer) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.localAddress(localAddress).option(ChannelOption.SO_KEEPALIVE, true);
        customizeBootstrap(bootstrap);
        bootstrap.group(this.workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        return super.createClient(remoteAddress, strategy, bootstrap, initializer);
    }
}
