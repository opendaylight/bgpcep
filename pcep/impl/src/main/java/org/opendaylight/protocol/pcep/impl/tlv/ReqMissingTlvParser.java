/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.tlv.ReqMissingTlv;
import org.opendaylight.protocol.util.ByteArray;

public class ReqMissingTlvParser implements PCEPTlvParser {
	
	public static final int TYPE = 3;
	
	private static final int REQ_ID_LENGTH = 4;

	@Override
	public PCEPTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		return new ReqMissingTlv(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, 0, REQ_ID_LENGTH)));
	}

	@Override
	public byte[] put(PCEPTlv obj) {
		byte[] valueBytes = new byte[REQ_ID_LENGTH];
		System.arraycopy(ByteArray.longToBytes(((ReqMissingTlv) obj).getRequestID()), Long.SIZE / Byte.SIZE - REQ_ID_LENGTH, valueBytes, 0, REQ_ID_LENGTH);
		return valueBytes;
	}
}
