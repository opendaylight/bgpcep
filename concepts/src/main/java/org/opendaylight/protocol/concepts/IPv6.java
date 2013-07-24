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

public final class IPv6 extends AbstractAddressFamily<IPv6Address> {
	private static final Pattern regex = Pattern.compile("/");
	public static final Set<AddressFamily<?>> FAMILIES;
	public static final IPv6 FAMILY;

	static {
		FAMILY = new IPv6();
		FAMILIES = new HashSet<>(1);
		FAMILIES.add(FAMILY);
	}

	private IPv6() { }

	@Override
	public IPv6Address addressForString(final String string) {
		Preconditions.checkNotNull(string, "String may not be null");
		return new IPv6Address(InetAddresses.forString(string));
	}

	@Override
	public IPv6Prefix prefixForString(final String string) {
		Preconditions.checkNotNull(string, "String may not be null");

		final String[] strs = regex.split(string);
		final byte len = Byte.valueOf(strs[1]);
		Preconditions.checkArgument(len >= 0 && len <= 128, "Prefix length has to be in range 0-128");
		return addressForString(strs[0]).asPrefix(len);
	}

	@Override
	public IPv6Address addressForBytes(byte[] bytes) {
		return new IPv6Address(bytes);
	}

	@Override
	public int requiredBytes() {
		return 16;
	}

	@Override
	public IPv6Prefix prefixForBytes(byte[] bytes, int bitLength) {
		Preconditions.checkArgument(bytes.length <= 16, "Byte array too large");
		Preconditions.checkArgument(bitLength <= 128, "Bitlength too large");

		if (bytes.length != 16)
			bytes = Arrays.copyOf(bytes, 16);

		return addressForBytes(bytes).asPrefix(bitLength);
	}
}

