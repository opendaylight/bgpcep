/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectWithTlvsParser;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.GcObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.GcBuilder;

/**
 * Parser for {@link GcObject}
 */
public class PCEPGlobalConstraintsObjectParser extends AbstractObjectWithTlvsParser<GcBuilder> {

	public static final int CLASS = 24;

	public static final int TYPE = 1;

	private final static int MAX_HOP_F_LENGTH = 1;
	private final static int MAX_UTIL_F_LENGTH = 1;
	private final static int MIN_UTIL_F_LENGTH = 1;
	private final static int OVER_BOOKING_FACTOR_F_LENGTH = 1;

	private final static int MAX_HOP_F_OFFSET = 0;
	private final static int MAX_UTIL_F_OFFSET = MAX_HOP_F_OFFSET + MAX_HOP_F_LENGTH;
	private final static int MIN_UTIL_F_OFFSET = MAX_UTIL_F_OFFSET + MAX_UTIL_F_LENGTH;
	private final static int OVER_BOOKING_FACTOR_F_OFFSET = MIN_UTIL_F_OFFSET + MIN_UTIL_F_LENGTH;

	private final static int TLVS_OFFSET = OVER_BOOKING_FACTOR_F_OFFSET + OVER_BOOKING_FACTOR_F_LENGTH;

	public PCEPGlobalConstraintsObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public GcObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}

		final GcBuilder builder = new GcBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setMaxHop((short) (bytes[MAX_HOP_F_OFFSET] & 0xFF));
		builder.setMinUtilization((short) (bytes[MIN_UTIL_F_OFFSET] & 0xFF));
		builder.setMaxUtilization((short) (bytes[MAX_UTIL_F_OFFSET] & 0xFF));
		builder.setOverBookingFactor((short) (bytes[OVER_BOOKING_FACTOR_F_OFFSET] & 0xFF));

		return builder.build();
	}

	@Override
	public void addTlv(final GcBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof GcObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed GcObject.");
		}

		final GcObject specObj = (GcObject) object;

		// final byte[] tlvs = PCEPTlvParser.put(specObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + 0];

		retBytes[MAX_HOP_F_OFFSET] = specObj.getMaxHop().byteValue();
		retBytes[MAX_UTIL_F_OFFSET] = specObj.getMaxUtilization().byteValue();
		retBytes[MIN_UTIL_F_OFFSET] = specObj.getMinUtilization().byteValue();
		retBytes[OVER_BOOKING_FACTOR_F_OFFSET] = specObj.getOverBookingFactor().byteValue();

		// ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		return retBytes;
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
