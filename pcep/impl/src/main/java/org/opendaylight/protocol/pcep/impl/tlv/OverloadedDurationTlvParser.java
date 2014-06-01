/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;

/**
 * Parser for {@link OverloadDuration}
 */
public class OverloadedDurationTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 2;

	private static final int OVERLOADED_DURATION_LENGTH = 4;

	@Override
	public OverloadDuration parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
		if (buffer == null) {
			return null;
		}
		return new OverloadDurationBuilder().setDuration(buffer.readUnsignedInt()).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("OverloadedTlv is mandatory.");
		}
		final OverloadDuration odt = (OverloadDuration) tlv;
		return TlvUtil.formatTlv(TYPE, ByteArray.longToBytes(odt.getDuration(), OVERLOADED_DURATION_LENGTH));
	}
}
