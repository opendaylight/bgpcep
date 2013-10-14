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
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.pcrep.pcrep.message.replies.result.failure.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.pcrep.pcrep.message.replies.result.failure.no.path.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPathBuilder;

/**
 * Parser for {@link NoPathObject}
 */
public class PCEPNoPathObjectParser extends AbstractObjectParser<NoPathBuilder> {

	public static final int CLASS = 3;

	public static final int TYPE = 1;

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

	public PCEPNoPathObjectParser(final SubobjectHandlerRegistry subobjReg, final TlvHandlerRegistry tlvReg) {
		super(subobjReg, tlvReg);
	}

	@Override
	public NoPathObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
	PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}

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
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof NoPathObject)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed NoPathObject.");
		}

		final NoPathObject nPObj = (NoPathObject) object;

		final byte[] tlvs = serializeTlvs(((NoPath) nPObj).getTlvs());
		int tlvsLength = 0;
		if (tlvs != null) {
			tlvsLength = tlvs.length;
		}
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvsLength + Util.getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		if (tlvs != null) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(C_FLAG_OFFSET, nPObj.isUnsatisfiedConstraints());
		retBytes[NI_F_OFFSET] = ByteArray.shortToBytes(nPObj.getNatureOfIssue())[1];
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs.getNoPathVector() != null) {
			// FIXME : add NoPath
			// return serializeTlv(new NoPathVectorBuilder().setFlags(tlvs.getNoPathVector()).build());
		}
		return null;
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
