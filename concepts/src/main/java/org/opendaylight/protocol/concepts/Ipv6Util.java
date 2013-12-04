/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;

/**
 * Util class for creating generated Ipv6Address.
 */
public final class Ipv6Util {
	private Ipv6Util() {
	}

	public static final int IPV6_LENGTH = 16;

	private static InetAddress getAddress(final byte[] bytes) {
		try {
			return Inet6Address.getByAddress(bytes);
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException("Failed to construct IPv6 address", e);
		}
	}

	public static Ipv6Address addressForBytes(final byte[] bytes) {
		return new Ipv6Address(InetAddresses.toAddrString(getAddress(bytes)));
	}

	public static byte[] bytesForAddress(final Ipv6Address address) {
		final InetAddress a = InetAddresses.forString(address.getValue());
		Preconditions.checkArgument(a instanceof Inet6Address);
		return a.getAddress();
	}

	public static byte[] bytesForPrefix(final Ipv6Prefix prefix) {
		final String p = prefix.getValue();
		final int sep = p.indexOf('/');
		final InetAddress a = InetAddresses.forString(p.substring(0, sep));
		Preconditions.checkArgument(a instanceof Inet6Address);
		final byte[] bytes = a.getAddress();
		return Bytes.concat(bytes, new byte[] { Byte.valueOf(p.substring(sep + 1, p.length())) });
	}

	public static Ipv6Prefix prefixForBytes(final byte[] bytes, final int length) {
		Preconditions.checkArgument(length <= bytes.length * Byte.SIZE);
		final byte[] tmp = Arrays.copyOfRange(bytes, 0, IPV6_LENGTH);
		final InetAddress a = getAddress(tmp);
		return new Ipv6Prefix(InetAddresses.toAddrString(a) + '/' + length);
	}

	public static List<Ipv6Prefix> prefixListForBytes(final byte[] bytes) {
		if (bytes.length == 0) {
			return Collections.emptyList();
		}

		final List<Ipv6Prefix> list = Lists.newArrayList();
		int byteOffset = 0;
		while (byteOffset < bytes.length) {
			final int bitLength = UnsignedBytes.toInt(ByteArray.subByte(bytes, byteOffset, 1)[0]);
			byteOffset += 1;
			final int byteCount = (bitLength % Byte.SIZE != 0) ? (bitLength / Byte.SIZE) + 1 : bitLength / Byte.SIZE;
			list.add(prefixForBytes(ByteArray.subByte(bytes, byteOffset, byteCount), bitLength));
			byteOffset += byteCount;
		}
		return list;
	}
}
