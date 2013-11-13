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
import org.opendaylight.protocol.pcep.tlv.P2MPCapabilityTlv;
import org.opendaylight.protocol.util.ByteArray;

public class P2MPCapabilityTlvParser implements PCEPTlvParser {
	
	public static final int TYPE = 6;
	
	private static final int P2MP_CAPABLITY_LENGTH = 2;

	@Override
	public PCEPTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		return new P2MPCapabilityTlv(ByteArray.bytesToShort(ByteArray.subByte(valueBytes, 0, P2MP_CAPABLITY_LENGTH)) & 0xFFFF);
	}

	@Override
	public byte[] put(PCEPTlv obj) {
		byte[] valueBytes = new byte[P2MP_CAPABLITY_LENGTH];
		ByteArray.copyWhole(ByteArray.shortToBytes((short) ((P2MPCapabilityTlv) obj).getValue()), valueBytes, 0);
		return valueBytes;
	}

}
