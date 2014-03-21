/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import io.netty.buffer.ByteBuf;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public class MessageUtil {

	private static final int VERSION_SF_LENGTH = 3;

	private MessageUtil() {
	}

	public static void formatMessage(final int messageType, final ByteBuf body, ByteBuf out) {
		final int msgLength = body.readableBytes();
		final byte[] header = new byte[] {
				UnsignedBytes.checkedCast(PCEPMessageConstants.PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH)),
				UnsignedBytes.checkedCast(messageType),
				UnsignedBytes.checkedCast((msgLength + PCEPMessageConstants.COMMON_HEADER_LENGTH) / 256),
				UnsignedBytes.checkedCast((msgLength + PCEPMessageConstants.COMMON_HEADER_LENGTH) % 256)
		};
		Preconditions.checkState(header.length == PCEPMessageConstants.COMMON_HEADER_LENGTH);
		out.writeBytes(header);
		out.writeBytes(body);
	}
}
