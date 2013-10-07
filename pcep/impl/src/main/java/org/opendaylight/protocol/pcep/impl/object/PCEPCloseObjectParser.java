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
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CCloseBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPCloseObject PCEPCloseObject}
 */
public class PCEPCloseObjectParser extends AbstractObjectParser<CCloseBuilder> {

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
	public static final int TLVS_F_OFFSET = REASON_F_OFFSET + REASON_F_LENGTH;

	public PCEPCloseObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public CloseObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		final CCloseBuilder builder = new CCloseBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_F_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setReason((short) (bytes[REASON_F_OFFSET] & 0xFF));

		return builder.build();
	}

	@Override
	public void addTlv(final CCloseBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof CClose))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPCloseObject.");

		final byte[] tlvs = PCEPTlvParser.put(((CClose) obj).getTlvs());
		final byte[] retBytes = new byte[TLVS_F_OFFSET + tlvs.length];
		ByteArray.copyWhole(tlvs, retBytes, TLVS_F_OFFSET);

		final int reason = ((CClose) obj).getReason().intValue();

		retBytes[REASON_F_OFFSET] = (byte) reason;

		return retBytes;
	}
}
