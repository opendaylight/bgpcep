/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import java.util.Collections;
import java.util.Set;

import org.opendaylight.protocol.util.ByteArray;
import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedBytes;

public abstract class AbstractAddressFamily<T extends NetworkAddress<?>> implements AddressFamily<T> {
	@Override
	public Set<Prefix<T>> prefixListForBytes(byte[] bytes) {
		if (bytes.length == 0)
			return Collections.emptySet();

		final Set<Prefix<T>> list = Sets.newHashSet();
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

