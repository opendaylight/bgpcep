/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import java.io.IOException;
import java.nio.channels.Channel;

import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Implementation of KeyAccess using Java Native Interface plugin to talk
 * directly to the underlying operating system.
 */
final class NativeKeyAccess implements KeyAccess {
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccess.class);

	private static native void setChannelKey0(Channel channel, byte[] key) throws IOException;
	static native boolean isClassSupported0(Class<?> channel);

	private final Channel channel;
	private byte[] key;

	NativeKeyAccess(final Channel channel) {
		this.channel = Preconditions.checkNotNull(channel);
	}

	@Override
	public void setKey(final byte[] key) throws IOException {
		Preconditions.checkArgument(key == null || key.length != 0, "Key may not be empty");

		synchronized (this) {
			setChannelKey0(channel, key);
			LOG.debug("Channel {} assigned key {}", channel, key);
			this.key = key;
		}
	}

	@Override
	public synchronized byte[] getKey() {
		return key;
	}
}
