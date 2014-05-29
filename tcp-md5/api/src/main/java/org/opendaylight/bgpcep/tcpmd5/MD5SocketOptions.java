/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.net.SocketOption;

/**
 * Utility class holding the singleton socket options used by the TCP-MD5 support
 * library.
 */
public final class MD5SocketOptions {
	/**
	 * TCP MD5 Signature option, as defined in RFC 2385.
	 */
	public static final SocketOption<byte[]> TCP_MD5SIG = new SocketOption<byte[]>() {
		@Override
		public String name() {
			return "TCP_MD5SIG";
		}

		@Override
		public Class<byte[]> type() {
			return byte[].class;
		}
	};

	private MD5SocketOptions() {
		throw new UnsupportedOperationException("Utility class cannot be instatiated");
	}
}
