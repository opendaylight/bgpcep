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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Implementation of KeyAccess using Java Native Interface plugin to talk
 * directly to the underlying operating system.
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

	private static native boolean isClassSupported0(Class<?> channel);
	private static native byte[] getChannelKey0(Channel channel) throws IOException;
	private static native void setChannelKey0(Channel channel, byte[] key) throws IOException;

	private final Channel channel;

	private NativeKeyAccess(final Channel channel) {
		this.channel = Preconditions.checkNotNull(channel);
	}

	public static KeyAccess create(final Channel channel) {
		if (isClassSupported0(channel.getClass())) {
			return new NativeKeyAccess(channel);
		} else {
			return null;
		}
	}

	public static boolean isAvailableForClass(final Class<?> clazz) {
		return isClassSupported0(clazz);
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
