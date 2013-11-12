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
import org.opendaylight.protocol.pcep.tlv.LSPStateDBVersionTlv;
import org.opendaylight.protocol.util.ByteArray;

public class LSPStateDBVersionTlvParser implements PCEPTlvParser {
	
	public static final int TYPE = 23;
	
	private static final int DBV_F_LENGTH = 8;

	@Override
	public PCEPTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		return new LSPStateDBVersionTlv(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, 0, DBV_F_LENGTH)));
	}

	@Override
	public byte[] put(PCEPTlv obj) {
		return ByteArray.longToBytes(((LSPStateDBVersionTlv) obj).getDbVersion());
	}
}
