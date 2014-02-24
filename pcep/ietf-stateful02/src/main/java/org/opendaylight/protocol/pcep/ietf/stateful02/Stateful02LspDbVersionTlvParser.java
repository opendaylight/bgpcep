/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import java.math.BigInteger;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

import com.google.common.base.Preconditions;

/**
 * Parser for {@link LspDbVersion}
 */
public final class Stateful02LspDbVersionTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 23;

	private static final int DBV_F_LENGTH = 8;

	@Override
	public LspDbVersion parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new LspDbVersionBuilder().setVersion(BigInteger.valueOf(ByteArray.bytesToLong(ByteArray.subByte(buffer, 0, DBV_F_LENGTH)))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		Preconditions.checkNotNull(tlv, "LspDbVersionTlv is mandatory.");
		final LspDbVersion lsp = (LspDbVersion) tlv;
		return ByteArray.longToBytes(lsp.getVersion().longValue(), DBV_F_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
