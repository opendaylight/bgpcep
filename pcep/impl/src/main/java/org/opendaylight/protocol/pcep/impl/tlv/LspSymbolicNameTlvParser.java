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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SymbolicPathNameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.SymblicPathNameBuilder;

/**
 * Parser for {@link SymbolicPathNameTlv}
 */
public class LspSymbolicNameTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 17;

	@Override
	public SymbolicPathNameTlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new SymblicPathNameBuilder().setPathName(buffer).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null)
			throw new IllegalArgumentException("SymbolicPathNameTlv is mandatory.");
		final SymbolicPathNameTlv spn = (SymbolicPathNameTlv) tlv;
		return spn.getPathName();
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
