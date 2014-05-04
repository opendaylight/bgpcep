/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;

import com.google.common.base.Preconditions;

/**
 * {@link NioServerSocketChannel} enabled with support for TCP MD5 Signature
 * option.
 */
public class MD5NioServerSocketChannel extends AbstractNioMessageChannel implements io.netty.channel.socket.ServerSocketChannel {
	private static final ChannelMetadata METADATA = new ChannelMetadata(false);

	private final MD5ServerSocketChannelConfig config;
	private final KeyAccessFactory keyAccessFactory;

	private static ServerSocketChannel newChannel() {
		try {
			return ServerSocketChannel.open();
		} catch (IOException e) {
			throw new ChannelException("Failed to instantiate channel", e);
		}
	}

	private MD5NioServerSocketChannel(final ServerSocketChannel channel, final KeyAccessFactory keyAccessFactory) {
		super(null, channel, SelectionKey.OP_ACCEPT);
		this.config = new DefaultMD5ServerSocketChannelConfig(this, keyAccessFactory);
		this.keyAccessFactory = Preconditions.checkNotNull(keyAccessFactory);
	}

	public MD5NioServerSocketChannel(final KeyAccessFactory keyAccessFactory) {
		this(newChannel(), keyAccessFactory);
	}

	@Override
	public MD5ServerSocketChannelConfig config() {
		return this.config;
	}

	@Override
	public InetSocketAddress localAddress() {
		return (InetSocketAddress) super.localAddress();
	}

	@Override
	public ChannelMetadata metadata() {
		return METADATA;
	}

	@Override
	public boolean isActive() {
		return javaChannel().socket().isBound();
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return null;
	}

	@Override
	protected ServerSocketChannel javaChannel() {
		return (ServerSocketChannel) super.javaChannel();
	}

	@Override
	protected SocketAddress localAddress0() {
		return javaChannel().socket().getLocalSocketAddress();
	}

	@Override
	protected void doBind(final SocketAddress localAddress) throws IOException {
		javaChannel().socket().bind(localAddress, config.getBacklog());
	}

	@Override
	protected void doClose() throws IOException {
		javaChannel().close();
	}

	@Override
	protected int doReadMessages(final List<Object> buf) throws IOException {
		final SocketChannel jc = javaChannel().accept();
		if (jc == null) {
			return 0;
		}

		final MD5NioSocketChannel ch = new MD5NioSocketChannel(this, jc, keyAccessFactory);
		buf.add(ch);
		return 1;
	}

	@Override
	protected boolean doWriteMessage(final Object msg, final ChannelOutboundBuffer in) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean doConnect(final SocketAddress remoteAddress,
			final SocketAddress localAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doFinishConnect() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected SocketAddress remoteAddress0() {
		return null;
	}

	@Override
	protected void doDisconnect() {
		throw new UnsupportedOperationException();
	}
}
