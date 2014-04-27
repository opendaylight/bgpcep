/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.channel.socket.nio.NioSocketChannel;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.nio.MD5SocketChannel;

/**
 * {@link NioSocketChannel} enabled with support for TCP MD5 Signature
 * option.
 */
public class MD5NioSocketChannel extends NioSocketChannel {
	private final MD5SocketChannelConfig config;
	private final MD5SocketChannel channel;

	public MD5NioSocketChannel() {
		super();
		this.channel = new MD5SocketChannel(super.javaChannel());
		this.config = new ProxyMD5SocketChannelConfig(super.config(), channel);
	}

	public MD5NioSocketChannel(final KeyAccessFactory keyAccessFactory) {
		super();
		this.channel = new MD5SocketChannel(super.javaChannel(), keyAccessFactory);
		this.config = new ProxyMD5SocketChannelConfig(super.config(), channel);
	}

	@Override
	public MD5SocketChannelConfig config() {
		return this.config;
	}

	@Override
	protected MD5SocketChannel javaChannel() {
		return this.channel;
	}
}
