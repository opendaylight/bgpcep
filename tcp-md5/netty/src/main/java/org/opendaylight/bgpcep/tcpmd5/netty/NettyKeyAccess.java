/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.channel.ChannelException;

import java.io.IOException;
import java.nio.channels.NetworkChannel;

import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;

import com.google.common.base.Preconditions;

/**
 * Utility class for handling MD5 option.
 */
final class NettyKeyAccess implements KeyAccess {
	private final KeyAccess delegate;

	private NettyKeyAccess(final KeyAccess delegate) {
		this.delegate = Preconditions.checkNotNull(delegate);
	}

	public static NettyKeyAccess create(final KeyAccessFactory factory, final NetworkChannel channel) {
		final KeyAccess access = factory.getKeyAccess(channel);
		return new NettyKeyAccess(access);
	}

	@Override
	public KeyMapping getKeys() {
		try {
			return delegate.getKeys();
		} catch (IOException e) {
			throw new ChannelException("Failed to set channel MD5 signature keys", e);
		}
	}

	@Override
	public void setKeys(final KeyMapping keys) {
		try {
			delegate.setKeys(keys);
		} catch (IOException e) {
			throw new ChannelException("Failed to set channel MD5 signature key", e);
		}
	}
}
