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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PredundancyGroupIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.PredundancyGroupIdBuilder;

/**
 * Parser for {@link PredundancyGroupIdTlv}
 */
public class PredundancyGroupTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 24;

	@Override
	public PredundancyGroupIdTlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new PredundancyGroupIdBuilder().setIdentifier(buffer).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null)
			throw new IllegalArgumentException("PredundancyGroupIdTlv is mandatory.");
		final PredundancyGroupIdTlv pgt = (PredundancyGroupIdTlv) tlv;
		return pgt.getIdentifier();
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
