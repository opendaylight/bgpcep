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
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.Tlvs;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPCloseObject PCEPCloseObject}
 */
public class PCEPCloseObjectParser extends AbstractObjectParser<CCloseBuilder> {

	public static final int CLASS = 15;

	public static final int TYPE = 1;

	/*
	 * lengths of fields in bytes
	 */
	public static final int FLAGS_F_LENGTH = 1;
	public static final int REASON_F_LENGTH = 1;

	/*
	 * offsets of fields in bytes
	 */
	public static final int FLAGS_F_OFFSET = 2; // added reserved field of size 2 bytes
	public static final int REASON_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

	/*
	 * total size of object in bytes
	 */
	public static final int TLVS_OFFSET = REASON_F_OFFSET + REASON_F_LENGTH;

	public PCEPCloseObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public CloseObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		final CCloseBuilder builder = new CCloseBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setReason((short) (bytes[REASON_F_OFFSET] & 0xFF));

		return builder.build();
	}

	@Override
	public void addTlv(final CCloseBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof CloseObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed CloseObject.");

		final CloseObject obj = (CloseObject) object;

		final byte[] tlvs = serializeTlvs(obj.getTlvs());
		int tlvsLength = 0;
		if (tlvs != null)
			tlvsLength = tlvs.length;
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvsLength + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		if (tlvs != null)
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		final int reason = ((CClose) obj).getReason().intValue();

		retBytes[REASON_F_OFFSET] = (byte) reason;

		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		// No tlvs defined
		return new byte[0];
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
