/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.opendaylight.bgpcep.tcpmd5.MD5ServerSocketChannel;

/**
 *
 */
public class MD5NioServerSocketChannel extends NioServerSocketChannel {
	private final MD5ServerSocketChannelConfig config;
	private final MD5ServerSocketChannel channel;

	public MD5NioServerSocketChannel() {
		super();
		this.channel = new MD5ServerSocketChannel(javaChannel());
		this.config = new ProxyMD5ServerSocketChannelConfig(super.config(), channel);
	}

	@Override
	public MD5ServerSocketChannelConfig config() {
		return this.config;
	}

	@Override
	protected MD5ServerSocketChannel javaChannel() {
		return channel;
	}
}
