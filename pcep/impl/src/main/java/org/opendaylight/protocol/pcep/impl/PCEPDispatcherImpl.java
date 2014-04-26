/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.bgpcep.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.bgpcep.tcpmd5.netty.MD5NioServerSocketChannelFactory;
import org.opendaylight.bgpcep.tcpmd5.netty.MD5NioSocketChannelFactory;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;

import com.google.common.base.Preconditions;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl extends AbstractDispatcher<PCEPSessionImpl, PCEPSessionListener> implements PCEPDispatcher, AutoCloseable {

	private final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> snf;
	private final PCEPHandlerFactory hf;
	private final KeyAccessFactory kaf;
	private KeyMapping keys;

	/**
	 * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
	 */
	public PCEPDispatcherImpl(final MessageRegistry registry,
			final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory, final EventLoopGroup bossGroup,
			final EventLoopGroup workerGroup) {
		this(registry, null, negotiatorFactory, bossGroup, workerGroup);
	}

	/**
	 * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
	 */
	public PCEPDispatcherImpl(final MessageRegistry registry, final KeyAccessFactory kaf,
			final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory, final EventLoopGroup bossGroup,
			final EventLoopGroup workerGroup) {
		super(bossGroup, workerGroup);
		this.kaf = kaf;
		this.snf = Preconditions.checkNotNull(negotiatorFactory);
		this.hf = new PCEPHandlerFactory(registry);
	}

	@Override
	public synchronized ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<PCEPSessionListener> listenerFactory) {
		return createServer(address, null, listenerFactory);
	}

	@Override
	public void close() {
	}

	@Override
	protected void customizeBootstrap(final Bootstrap b) {
		if (keys != null) {
			b.channelFactory(new MD5NioSocketChannelFactory(kaf));
			b.option(MD5ChannelOption.TCP_MD5SIG, keys);
		}
	}

	@Override
	protected void customizeBootstrap(final ServerBootstrap b) {
		if (keys != null) {
			b.channelFactory(new MD5NioServerSocketChannelFactory(kaf));
			b.option(MD5ChannelOption.TCP_MD5SIG, keys);
		}
	}

	@Override
	public synchronized ChannelFuture createServer(final InetSocketAddress address, final KeyMapping keys,
			final SessionListenerFactory<PCEPSessionListener> listenerFactory) {
		if (keys != null && !keys.isEmpty() && kaf == null) {
			throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
		}

		this.keys = keys;
		ChannelFuture ret = super.createServer(address, new PipelineInitializer<PCEPSessionImpl>() {
			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
				ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getDecoders());
				ch.pipeline().addLast("negotiator", PCEPDispatcherImpl.this.snf.getSessionNegotiator(listenerFactory, ch, promise));
				ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getEncoders());
			}
		});

		this.keys = null;
		return ret;
	}
}
