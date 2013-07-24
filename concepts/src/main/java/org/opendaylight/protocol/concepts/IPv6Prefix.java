/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import javax.annotation.concurrent.Immutable;

/**
 * IPv6 Address prefix.
 */
@Immutable
public class IPv6Prefix extends AbstractPrefix<IPv6Address> {
	private static final long serialVersionUID = 8936908223539148352L;

	/**
	 * Create a new IPv6 prefix using an IPv6 address and prefix
	 * length.
	 *
	 * @param address IPv6 address {@link IPv6Address}
	 * @param length Prefix length
	 */
	public IPv6Prefix(final IPv6Address address, final int length) {
		super(address, length);
	}

	@Override
	public IPv6 getAddressFamily() {
		return IPv6.FAMILY;
	}
}
