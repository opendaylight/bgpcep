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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.OrderBuilder;

/**
 * Parser for {@link Order}
 */
public class OrderTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 5;

	private static final int ORDR_DEL_LENGTH = 4;

	private static final int ORDR_SETUP_LENGTH = 4;

	@Override
	public Order parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		return new OrderBuilder().setDelete(Long.valueOf(ByteArray.bytesToLong(ByteArray.subByte(buffer, 0, ORDR_DEL_LENGTH)))).setSetup(
				ByteArray.bytesToLong(ByteArray.subByte(buffer, ORDR_DEL_LENGTH, ORDR_SETUP_LENGTH))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("OrderTlv is mandatory.");
		}
		final Order otlv = (Order) tlv;
		final byte[] bytes = new byte[ORDR_DEL_LENGTH + ORDR_SETUP_LENGTH];
		int offset = 0;
		ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(otlv.getDelete()), 4, ORDR_DEL_LENGTH), bytes, offset);
		offset += ORDR_DEL_LENGTH;
		ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(otlv.getSetup()), 4, ORDR_SETUP_LENGTH), bytes, offset);
		return bytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
