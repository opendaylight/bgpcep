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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.tlvs.ReqMissingBuilder;

/**
 * Parser {@link ReqMissingTlv}
 */
public class ReqMissingTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 3;

	private static final int REQ_ID_LENGTH = 4;

	@Override
	public ReqMissingTlv parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new ReqMissingBuilder().setRequestId(new RequestId(ByteArray.bytesToLong(ByteArray.subByte(buffer, 0, REQ_ID_LENGTH)))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("ReqMissingTlv is mandatory.");
		}
		final ReqMissingTlv req = (ReqMissingTlv) tlv;
		return ByteArray.subByte(ByteArray.longToBytes(req.getRequestId().getValue()), REQ_ID_LENGTH, REQ_ID_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
