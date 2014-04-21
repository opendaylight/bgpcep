/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;

import com.google.common.base.Preconditions;

/**
 * {@link ServerSocketChannel} augmented with support for TCP MD5 Signature
 * option.
 */
public final class MD5ServerSocketChannel extends ServerSocketChannel {
	private final KeyAccessFactory keyAccessFactory;
	private final ServerSocketChannel inner;
	private final MD5ChannelOptions options;

	public MD5ServerSocketChannel(final ServerSocketChannel inner, final KeyAccessFactory keyAccessFactory) {
		super(inner.provider());
		this.inner = inner;
		this.keyAccessFactory = Preconditions.checkNotNull(keyAccessFactory);
		this.options = MD5ChannelOptions.create(keyAccessFactory, inner);
	}

	public MD5ServerSocketChannel(final ServerSocketChannel inner) {
		this(inner, DefaultKeyAccessFactoryFactory.getKeyAccessFactory());
	}

	public static MD5ServerSocketChannel open() throws IOException {
		return new MD5ServerSocketChannel(ServerSocketChannel.open());
	}

	@Override
	public SocketAddress getLocalAddress() throws IOException {
		return inner.getLocalAddress();
	}

	@Override
	public <T> T getOption(final SocketOption<T> name) throws IOException {
		return options.getOption(name);
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		return options.supportedOptions();
	}

	@Override
	public ServerSocketChannel bind(final SocketAddress local, final int backlog) throws IOException {
		inner.bind(local, backlog);
		return this;
	}

	@Override
	public <T> ServerSocketChannel setOption(final SocketOption<T> name, final T value) throws IOException {
		options.setOption(name, value);
		return this;
	}

	@Override
	public ServerSocket socket() {
		// FIXME: provider a wrapper
		return inner.socket();
	}

	@Override
	public MD5SocketChannel accept() throws IOException {
		return new MD5SocketChannel(inner.accept(), keyAccessFactory);
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
		inner.close();
	}

	@Override
	protected void implConfigureBlocking(final boolean block) throws IOException {
		inner.configureBlocking(block);
	}
}
