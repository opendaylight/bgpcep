/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import com.google.common.primitives.UnsignedBytes;

public final class CapabilityUtil {

	public static final int CODE_SIZE = 1; // bytes
	public static final int LENGTH_SIZE = 1; // bytes

	private CapabilityUtil() {

	}

	public static ByteBuf formatCapability(final int code, final byte[] value) {
		final CompositeByteBuf ret = Unpooled.compositeBuffer(2);
		ret.addComponent(Unpooled.wrappedBuffer(new byte[] {
				UnsignedBytes.checkedCast(code),
				UnsignedBytes.checkedCast(value.length), }));
		ret.addComponent(Unpooled.wrappedBuffer(value));
		return ret;
	}
}
