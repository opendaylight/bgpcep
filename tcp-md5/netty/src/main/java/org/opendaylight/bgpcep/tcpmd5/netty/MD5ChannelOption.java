/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.channel.ChannelOption;

/**
 *
 */
public final class MD5ChannelOption extends ChannelOption<byte[]> {
	public static final MD5ChannelOption TCP_MD5SIG = new MD5ChannelOption("TCP_MD5SIG");

	private MD5ChannelOption(final String name) {
		super(name);
	}
}
