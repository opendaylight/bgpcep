/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.io.IOException;
import java.net.SocketOption;
import java.nio.channels.NetworkChannel;
import java.util.Set;

import org.opendaylight.bgpcep.tcpmd5.jni.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.jni.NativeKeyAccess;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 *
 */
final class MD5ChannelOptions {
	private static final Set<SocketOption<?>> md5Options = ImmutableSet.<SocketOption<?>>of(SocketOptions.TCP_MD5SIG);
	private final NetworkChannel ch;
	private final KeyAccess access;

	private MD5ChannelOptions(final NetworkChannel ch, final KeyAccess access) {
		this.ch = Preconditions.checkNotNull(ch);
		this.access = access;
	}

	static MD5ChannelOptions create(final NetworkChannel ch) {
		final KeyAccess access = NativeKeyAccess.create(ch);
		return new MD5ChannelOptions(ch, access);
	}

	public <T> T getOption(final SocketOption<T> name) throws IOException {
		if (access != null && name.equals(SocketOptions.TCP_MD5SIG)) {
			@SuppressWarnings("unchecked")
			final T key = (T)access.getKey();
			return key;
		}

		return ch.getOption(name);
	}

	public <T> void setOption(final SocketOption<T> name, final T value) throws IOException {
		if (access != null && name.equals(SocketOptions.TCP_MD5SIG)) {
			access.setKey((byte[])value);
		} else {
			ch.setOption(name, value);
		}
	}

	public Set<SocketOption<?>> supportedOptions() {
		if (access != null) {
			return Sets.union(md5Options, ch.supportedOptions());
		} else {
			return ch.supportedOptions();
		}
	}
}
