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
import org.opendaylight.protocol.pcep.tlv.OrderTlv;
import org.opendaylight.protocol.util.ByteArray;

public class OrderTlvParser implements PCEPTlvParser {
	
	public static final int TYPE = 5;
	
	private static final int ORDR_DEL_LENGTH = 4;
    private static final int ORDR_SETUP_LENGTH = 4;

	@Override
	public PCEPTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		return new OrderTlv(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, 0, ORDR_DEL_LENGTH)), ByteArray.bytesToLong(ByteArray.subByte(
				valueBytes, ORDR_DEL_LENGTH, ORDR_SETUP_LENGTH)));
	}

	@Override
	public byte[] put(PCEPTlv obj) {
		byte[] valueBytes = new byte[ORDR_DEL_LENGTH + ORDR_SETUP_LENGTH];
		ByteArray.copyWhole(ByteArray.intToBytes((int) ((OrderTlv) obj).getDeleteOrder()), valueBytes, 0);
		ByteArray.copyWhole(ByteArray.intToBytes((int) ((OrderTlv) obj).getSetupOrder()), valueBytes, ORDR_DEL_LENGTH);
		return valueBytes;
	}
}
