/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;

public final class IPv4 extends AbstractAddressFamily<IPv4Address> {
	private static final Pattern regex = Pattern.compile("/");
	public static final Set<AddressFamily<?>> FAMILIES;
	public static final IPv4 FAMILY;

	static {
		FAMILY = new IPv4();
		FAMILIES = new HashSet<>(1);
		FAMILIES.add(FAMILY);
	}

	private IPv4() { }

	@Override
	public IPv4Address addressForString(final String string) {
		Preconditions.checkNotNull(string, "String may not be null");
		return new IPv4Address(InetAddresses.forString(string));
	}

	@Override
	public IPv4Prefix prefixForString(final String string) {
		Preconditions.checkNotNull(string, "String may not be null");

		final String[] strs = regex.split(string);
		Preconditions.checkArgument(strs.length == 2, "Malformed prefix string");
		final byte len = Byte.valueOf(strs[1]);
		Preconditions.checkArgument(len >= 0 && len <= 32, "Prefix length has to be in range 0-32");
		return addressForString(strs[0]).asPrefix(len);
	}

	@Override
	public IPv4Address addressForBytes(byte[] bytes) {
		return new IPv4Address(bytes);
	}

	@Override
	public int requiredBytes() {
		return 4;
	}

	@Override
	public IPv4Prefix prefixForBytes(byte[] bytes, int bitLength) {
		Preconditions.checkArgument(bytes.length <= 4, "Byte array too large");
		Preconditions.checkArgument(bitLength <= 32, "Bitlength too large");

		if (bytes.length != 4)
			bytes = Arrays.copyOf(bytes, 4);

		return addressForBytes(bytes).asPrefix(bitLength);
	}
}
