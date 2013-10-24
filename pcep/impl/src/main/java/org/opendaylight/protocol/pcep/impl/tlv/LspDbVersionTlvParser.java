/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.math.BigInteger;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspDbVersionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.LspDbVersionBuilder;

/**
 * Parser for {@link LspDbVersionTlv}
 */
public class LspDbVersionTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 23;

	private static final int DBV_F_LENGTH = 8;

	@Override
	public LspDbVersionTlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new LspDbVersionBuilder().setVersion(BigInteger.valueOf(ByteArray.bytesToLong(ByteArray.subByte(buffer, 0, DBV_F_LENGTH)))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("LspDbVersionTlv is mandatory.");
		}
		final LspDbVersionTlv lsp = (LspDbVersionTlv) tlv;
		return ByteArray.subByte(lsp.getVersion().toByteArray(), 0, DBV_F_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
