/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

/**
 * IPv4 Address prefix.
 */
public class IPv4Prefix extends AbstractPrefix<IPv4Address> {

	private static final long serialVersionUID = 2206353300109616995L;

	/**
	 * Construct an IPv4 prefix given a base IPv4 address and prefix
	 * length.
	 *
	 * @param address Base address
	 * @param length Prefix length, as to be between 0-32.
	 */
	public IPv4Prefix(final IPv4Address address, final int length) {
		super(address, length);
	}

	@Override
	public IPv4 getAddressFamily() {
		return IPv4.FAMILY;
	}
}

