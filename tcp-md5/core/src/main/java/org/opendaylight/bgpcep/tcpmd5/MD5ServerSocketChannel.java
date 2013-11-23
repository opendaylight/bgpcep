/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

/**
 *
 */
public final class MD5ServerSocketChannel extends ServerSocketChannel {
	private final ServerSocketChannel inner;
	private final MD5ChannelOptions options;

	public MD5ServerSocketChannel(final ServerSocketChannel inner) {
		super(inner.provider());
		this.options = MD5ChannelOptions.create(inner);
		this.inner = inner;
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
		return new MD5SocketChannel(inner.accept());
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
