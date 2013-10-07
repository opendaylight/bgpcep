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
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.tlvs.OrderBuilder;

public class OrderTlvParser implements TlvParser {
	
    private static final int ORDR_DEL_LENGTH = 4;
    
    private static final int ORDR_SETUP_LENGTH = 4;

	@Override
	public OrderTlv parseTlv(byte[] buffer) throws PCEPDeserializerException {
		return new OrderBuilder().setDelete(Long.valueOf(ByteArray.bytesToLong(ByteArray.subByte(buffer, 0, ORDR_DEL_LENGTH)))).setSetup(ByteArray.bytesToLong(ByteArray.subByte(
				buffer, ORDR_DEL_LENGTH, ORDR_SETUP_LENGTH))).build();
	}
}
