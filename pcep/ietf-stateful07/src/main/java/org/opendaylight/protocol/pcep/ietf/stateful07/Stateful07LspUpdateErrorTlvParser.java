/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link LspErrorCode}
 */
public final class Stateful07LspUpdateErrorTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 20;

	private static final int UPDATE_ERR_CODE_LENGTH = 4;

	@Override
	public LspErrorCode parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new LspErrorCodeBuilder().setErrorCode(ByteArray.bytesToLong(buffer)).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("LspErrorCodeTlv is mandatory.");
		}
		final LspErrorCode lsp = (LspErrorCode) tlv;
		return ByteArray.longToBytes(lsp.getErrorCode(), UPDATE_ERR_CODE_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
