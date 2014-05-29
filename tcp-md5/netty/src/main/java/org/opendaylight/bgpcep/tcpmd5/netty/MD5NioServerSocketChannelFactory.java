/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.bootstrap.ChannelFactory;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;

import com.google.common.base.Preconditions;

public final class MD5NioServerSocketChannelFactory implements ChannelFactory<MD5NioServerSocketChannel> {
	private final KeyAccessFactory keyAccessFactory;

	public MD5NioServerSocketChannelFactory(final KeyAccessFactory keyAccessFactory) {
		this.keyAccessFactory = Preconditions.checkNotNull(keyAccessFactory);
	}

	@Override
	public MD5NioServerSocketChannel newChannel() {
		return new MD5NioServerSocketChannel(keyAccessFactory);
	}
}
