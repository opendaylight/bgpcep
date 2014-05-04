/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.FileRegion;
import io.netty.channel.nio.AbstractNioByteChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;

import com.google.common.base.Preconditions;

/**
 * {@link io.netty.channel.socket.nio.NioSocketChannel} enabled with support
 * for TCP MD5 Signature option.
 */
public class MD5NioSocketChannel extends AbstractNioByteChannel implements io.netty.channel.socket.SocketChannel {
	private static final ChannelMetadata METADATA = new ChannelMetadata(false);

	private final MD5SocketChannelConfig config;

	private static SocketChannel newChannel() {
		try {
			return SocketChannel.open();
		} catch (IOException e) {
			throw new ChannelException("Failed to instantiate channel", e);
		}
	}

	MD5NioSocketChannel(final MD5NioServerSocketChannel parent, final SocketChannel channel, final KeyAccessFactory factory) {
		super(parent, channel);
		this.config = new DefaultMD5SocketChannelConfig(this, factory);
	}

	public MD5NioSocketChannel(final KeyAccessFactory keyAccessFactory) {
		this(null, newChannel(), keyAccessFactory);
	}

	@Override
	public MD5SocketChannelConfig config() {
		return this.config;
	}

	@Override
	protected SocketChannel javaChannel() {
		return (SocketChannel) super.javaChannel();
	}

	@Override
	public boolean isActive() {
		final SocketChannel ch = javaChannel();
		return ch.isOpen() && ch.isConnected();
	}

	@Override
	public ChannelMetadata metadata() {
		return METADATA;
	}

	@Override
	public MD5NioServerSocketChannel parent() {
		return (MD5NioServerSocketChannel) super.parent();
	}

	@Override
	public InetSocketAddress localAddress() {
		return (InetSocketAddress) super.localAddress();
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return (InetSocketAddress) super.remoteAddress();
	}

	@Override
	public boolean isInputShutdown() {
		return super.isInputShutdown();
	}

	@Override
	public boolean isOutputShutdown() {
		return javaChannel().socket().isOutputShutdown() || !isActive();
	}

	@Override
	public ChannelFuture shutdownOutput() {
		return shutdownOutput(newPromise());
	}

	@Override
	public ChannelFuture shutdownOutput(final ChannelPromise future) {
		EventLoop loop = eventLoop();
		if (loop.inEventLoop()) {
			try {
				javaChannel().socket().shutdownOutput();
				future.setSuccess();
			} catch (Exception e) {
				future.setFailure(e);
			}
		} else {
			loop.execute(new Runnable() {
				@Override
				public void run() {
					shutdownOutput(future);
				}
			});
		}
		return future;
	}

	@Override
	protected long doWriteFileRegion(final FileRegion region) throws IOException {
		return region.transferTo(javaChannel(), region.transfered());
	}

	@Override
	protected int doReadBytes(final ByteBuf buf) throws IOException {
		return buf.writeBytes(javaChannel(), buf.writableBytes());
	}

	@Override
	protected int doWriteBytes(final ByteBuf buf) throws IOException {
		return buf.readBytes(javaChannel(), buf.readableBytes());
	}

	@Override
	protected boolean doConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) throws IOException {
		if (localAddress != null) {
			javaChannel().socket().bind(localAddress);
		}

		boolean success = false;
		try {
			boolean connected = javaChannel().connect(remoteAddress);
			if (!connected) {
				selectionKey().interestOps(SelectionKey.OP_CONNECT);
			}
			success = true;
			return connected;
		} finally {
			if (!success) {
				doClose();
			}
		}
	}

	@Override
	protected void doFinishConnect() throws IOException {
		Preconditions.checkState(javaChannel().finishConnect() == true, "finishConnect() failed");
	}

	@Override
	protected SocketAddress localAddress0() {
		return javaChannel().socket().getLocalSocketAddress();
	}

	@Override
	protected SocketAddress remoteAddress0() {
		return javaChannel().socket().getRemoteSocketAddress();
	}

	@Override
	protected void doBind(final SocketAddress localAddress) throws IOException {
		javaChannel().socket().bind(localAddress);
	}

	@Override
	protected void doDisconnect() throws IOException {
		doClose();
	}

	@Override
	protected void doClose() throws IOException {
		javaChannel().close();
	}
}
