/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPathBuilder;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPNoPathObject PCEPNoPathObject}
 */
public class PCEPNoPathObjectParser extends AbstractObjectParser<NoPathBuilder> {

	/*
	 * lengths of fields in bytes
	 */
	public static final int NI_F_LENGTH = 1; // multi-field
	public static final int FLAGS_F_LENGTH = 2;
	public static final int RESERVED_F_LENGTH = 1;

	/*
	 * offsets of field in bytes
	 */

	public static final int NI_F_OFFSET = 0;
	public static final int FLAGS_F_OFFSET = NI_F_OFFSET + NI_F_LENGTH;
	public static final int RESERVED_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int TLVS_OFFSET = RESERVED_F_OFFSET + RESERVED_F_LENGTH;

	/*
	 * defined flags
	 */

	public static final int C_FLAG_OFFSET = 0;

	public PCEPNoPathObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public NoPath parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, FLAGS_F_OFFSET, FLAGS_F_LENGTH));

		final NoPathBuilder builder = new NoPathBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setNatureOfIssue((short) (bytes[NI_F_OFFSET] & 0xFF));
		builder.setUnsatisfiedConstraints(flags.get(C_FLAG_OFFSET));

		return builder.build();
	}

	@Override
	public void addTlv(final NoPathBuilder builder, final Tlv tlv) {
		// FIXME : add no-path-vector-tlv
	}

	@Override
	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPNoPathObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPNoPathObject.");

		final PCEPNoPathObject nPObj = (PCEPNoPathObject) obj;

		final byte[] tlvs = PCEPTlvParser.put(nPObj.getTlvs());
		final byte[] retBytes = new byte[tlvs.length + TLVS_OFFSET];
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(C_FLAG_OFFSET, nPObj.isConstrained());
		retBytes[NI_F_OFFSET] = ByteArray.shortToBytes(nPObj.getNatureOfIssue())[1];
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		return retBytes;
	}
}
