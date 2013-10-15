/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * Util class for creating generated Ipv4Address.
 */
public final class Ipv4Util {

	public static Ipv4Address addressForBytes(final byte[] bytes) {
		try {
			return new Ipv4Address(Inet4Address.getByAddress(bytes).getHostAddress());
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public static byte[] bytesForAddress(final Ipv4Address address) {
		Inet4Address a;
		try {
			a = (Inet4Address) InetAddress.getByName(address.getValue());
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		return a.getAddress();
	}

	public static Ipv4Prefix prefixForBytes(final byte[] bytes, final int length) {
		Preconditions.checkArgument(length <= bytes.length * 8);
		return new Ipv4Prefix(addressForBytes(bytes).toString() + "/" + length);
	}

	public static List<Ipv4Prefix> prefixListForBytes(final byte[] bytes) {
		if (bytes.length == 0)
			return Collections.emptyList();

		final List<Ipv4Prefix> list = Lists.newArrayList();
		int byteOffset = 0;
		while (byteOffset < bytes.length) {
			final int bitLength = UnsignedBytes.toInt(ByteArray.subByte(bytes, byteOffset, 1)[0]);
			byteOffset += 1;
			final int byteCount = (bitLength % 8 != 0) ? (bitLength / 8) + 1 : bitLength / 8;
			list.add(prefixForBytes(ByteArray.subByte(bytes, byteOffset, byteCount), bitLength));
			byteOffset += byteCount;
		}
		return list;
	}

	public static int getPrefixLength(final IpPrefix prefix) {
		String p = "";
		if (prefix.getIpv4Prefix() != null) {
			p = prefix.getIpv4Prefix().getValue();
		} else {
			p = prefix.getIpv6Prefix().getValue();
		}
		final int sep = p.indexOf("/");
		return Integer.valueOf(p.substring(sep, p.length() - 1));
	}
}
