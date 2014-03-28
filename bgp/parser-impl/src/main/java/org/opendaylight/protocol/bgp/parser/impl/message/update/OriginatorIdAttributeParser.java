/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;

import com.google.common.base.Preconditions;

public final class OriginatorIdAttributeParser implements AttributeParser {
	public static final int TYPE = 9;

	private static final int ORIGINATOR_LENGTH = 4;

	@Override
	public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) {
		Preconditions.checkArgument(buffer.readableBytes() == ORIGINATOR_LENGTH, "Length of byte array for ORIGINATOR_ID should be %s, but is %s", ORIGINATOR_LENGTH, buffer.readableBytes());
		builder.setOriginatorId(Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, ORIGINATOR_LENGTH)));
	}
}