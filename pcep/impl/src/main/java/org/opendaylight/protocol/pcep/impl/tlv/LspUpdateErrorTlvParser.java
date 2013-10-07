/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspErrorCodeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.LspErrorCodeBuilder;

/**
 * Parser for {@link LspErrorCodeTlv}
 */
public class LspUpdateErrorTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 20;

	private static final int UPDATE_ERR_CODE_LENGTH = 4;

	@Override
	public LspErrorCodeTlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new LspErrorCodeBuilder().setErrorCode(ByteArray.bytesToLong(buffer)).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null)
			throw new IllegalArgumentException("LspErrorCodeTlv is mandatory.");
		final LspErrorCodeTlv lsp = (LspErrorCodeTlv) tlv;
		return ByteArray.subByte(ByteArray.longToBytes(lsp.getErrorCode()), 0, UPDATE_ERR_CODE_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
