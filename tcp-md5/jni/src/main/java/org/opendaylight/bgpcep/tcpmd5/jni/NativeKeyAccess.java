/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.Map.Entry;

import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Implementation of KeyAccess using Java Native Interface plugin to talk
 * directly to the underlying operating system.
 */
final class NativeKeyAccess implements KeyAccess {
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccess.class);

	private static native void setChannelKey0(Channel channel, byte[] address, byte[] key) throws IOException;
	static native boolean isClassSupported0(Class<?> channel);

	private KeyMapping currentKeys = new KeyMapping();
	private final Channel channel;

	NativeKeyAccess(final Channel channel) {
		this.channel = Preconditions.checkNotNull(channel);
	}

	@Override
	public void setKeys(final KeyMapping keys) throws IOException {
		for (Entry<InetAddress, byte[]> e : keys.entrySet()) {
			Preconditions.checkArgument(e.getKey() != null, "Null address is not allowed");
			Preconditions.checkArgument(e.getValue() != null, "Key for address %s is null", e.getKey());
			Preconditions.checkArgument(e.getValue().length != 0, "Key for address %s is empty", e.getKey());
		}

		synchronized (this) {
			// Walk current keys and delete any that are not in the new map
			for (Entry<InetAddress, byte[]> e : currentKeys.entrySet()) {
				if (!keys.containsKey(e.getKey())) {
					LOG.debug("Channel {} removing key for address {}", channel, e.getKey());
					setChannelKey0(channel, e.getKey().getAddress(), null);
				}
			}

			for (Entry<InetAddress, byte[]> e : keys.entrySet()) {
				final byte[] currentValue = currentKeys.get(e.getKey());
				if (!Arrays.equals(currentValue, e.getValue())) {
					LOG.debug("Channel {} adding key for address {}", channel, e.getKey());
					setChannelKey0(channel, e.getKey().getAddress(), e.getValue());
				}
			}

			this.currentKeys = new KeyMapping(keys);
			LOG.debug("Channel {} key mapping update completed", channel);
		}
	}

	@Override
	public synchronized KeyMapping getKeys() {
		return currentKeys;
	}
}
