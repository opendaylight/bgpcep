/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * Util class for creating generated Ipv6Address.
 */
public class Ipv6Util {

	public static Ipv6Address addressForBytes(final byte[] bytes) {
		try {
			return new Ipv6Address(Inet6Address.getByAddress(bytes).toString());
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public static Ipv6Prefix prefixForBytes(final byte[] bytes, final int length) {
		Preconditions.checkArgument(length <= bytes.length * 8);
		return new Ipv6Prefix(addressForBytes(bytes).toString() + "/" + length);
	}

	public static List<Ipv6Prefix> prefixListForBytes(final byte[] bytes) {
		if (bytes.length == 0)
			return Collections.emptyList();

		final List<Ipv6Prefix> list = Lists.newArrayList();
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
}
