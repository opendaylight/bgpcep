/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.crabbe.initiated00;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public final class LSPCleanupTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 26;

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		return ByteArray.intToBytes(((LspCleanup) tlv).getTimeout().intValue());
	}

	@Override
	public Tlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new LspCleanupBuilder().setTimeout(ByteArray.bytesToLong(buffer)).build();
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
