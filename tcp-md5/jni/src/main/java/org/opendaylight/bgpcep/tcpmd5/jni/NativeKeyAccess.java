/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import java.io.IOException;
import java.nio.channels.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class NativeKeyAccess implements KeyAccess {
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccess.class);

	static {
		NarSystem.loadLibrary();

		int rt = NarSystem.runUnitTests();
		if (rt == 0) {
			LOG.warn("Run-time initialization failed");
		} else {
			LOG.debug("Run-time found {} supported channel classes", rt);
		}
	}

	private final Channel channel;

	private NativeKeyAccess(final Channel channel) {
		this.channel = Preconditions.checkNotNull(channel);
	}

	private static native boolean isChannelSupported0(Channel channel);
	private static native byte[] getChannelKey0(Channel channel) throws IOException;
	private static native void setChannelKey0(Channel channel, byte[] key) throws IOException;

	public static KeyAccess create(final Channel channel) {
		if (isChannelSupported0(channel)) {
			return new NativeKeyAccess(channel);
		} else {
			return null;
		}
	}

	@Override
	public byte[] getKey() throws IOException {
		synchronized (channel) {
			return getChannelKey0(channel);
		}
	}

	@Override
	public void setKey(final byte[] key) throws IOException {
		synchronized (channel) {
			setChannelKey0(channel, Preconditions.checkNotNull(key));
		}
	}
}
