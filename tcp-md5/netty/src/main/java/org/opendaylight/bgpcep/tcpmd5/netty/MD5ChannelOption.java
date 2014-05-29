/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.channel.ChannelOption;

import com.google.common.base.Preconditions;

/**
 * TCP MD5 Signature {@link ChannelOption}.
 */
public final class MD5ChannelOption extends ChannelOption<byte[]> {
	/**
	 * Singleton instance of TCP MD5 Signature ChannelOption.
	 */
	public static final MD5ChannelOption TCP_MD5SIG = new MD5ChannelOption("TCP_MD5SIG");

	private MD5ChannelOption(final String name) {
		super(name);
	}

	@Override
	public void validate(final byte[] value) {
		// null value is allowed
		Preconditions.checkArgument(value == null || value.length != 0);
	}
}
