/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;

/**
 * {@link SocketChannel} augmented with support for TCP MD5 Signature option.
 */
public final class MD5SocketChannel extends SocketChannel {
	private final MD5ChannelOptions options;
	private final SocketChannel inner;

	public MD5SocketChannel(final SocketChannel inner) {
		this(inner, DefaultKeyAccessFactoryFactory.getKeyAccessFactory());
	}

	public MD5SocketChannel(final SocketChannel inner, final KeyAccessFactory keyAccessFactory) {
		super(inner.provider());
		this.inner = inner;
		options = MD5ChannelOptions.create(keyAccessFactory, inner);
	}

	public static MD5SocketChannel open() throws IOException {
		return new MD5SocketChannel(SocketChannel.open());
	}

	public static MD5SocketChannel open(final KeyAccessFactory keyAccessFactory) throws IOException {
		return new MD5SocketChannel(SocketChannel.open(), keyAccessFactory);
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
	public SocketChannel bind(final SocketAddress local) throws IOException {
		inner.bind(local);
		return this;
	}

	@Override
	public <T> SocketChannel setOption(final SocketOption<T> name, final T value) throws IOException {
		options.setOption(name, value);
		return this;
	}

	@Override
	public SocketChannel shutdownInput() throws IOException {
		inner.shutdownInput();
		return this;
	}

	@Override
	public SocketChannel shutdownOutput() throws IOException {
		inner.shutdownOutput();
		return this;
	}

	@Override
	public Socket socket() {
		// FIXME: provide a wrapper
		return inner.socket();
	}

	@Override
	public boolean isConnected() {
		return inner.isConnected();
	}

	@Override
	public boolean isConnectionPending() {
		return inner.isConnectionPending();
	}

	@Override
	public boolean connect(final SocketAddress remote) throws IOException {
		return inner.connect(remote);
	}

	@Override
	public boolean finishConnect() throws IOException {
		return inner.finishConnect();
	}

	@Override
	public SocketAddress getRemoteAddress() throws IOException {
		return inner.getRemoteAddress();
	}

	@Override
	public int read(final ByteBuffer dst) throws IOException {
		return inner.read(dst);
	}

	@Override
	public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
		return inner.read(dsts, offset, length);
	}

	@Override
	public int write(final ByteBuffer src) throws IOException {
		return inner.write(src);
	}

	@Override
	public long write(final ByteBuffer[] srcs, final int offset, final int length) throws IOException {
		return inner.write(srcs, offset, length);
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
