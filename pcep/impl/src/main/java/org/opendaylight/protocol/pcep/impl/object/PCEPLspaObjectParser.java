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
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspaObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

/**
 * Parser for {@link LspaObject}
 */
public class PCEPLspaObjectParser extends AbstractObjectParser<LspaBuilder> {

	public static final int CLASS = 9;

	public static final int TYPE = 1;

	/*
	 * lengths of fields in bytes
	 */
	public static final int EXC_ANY_F_LENGTH = 4;
	public static final int INC_ANY_F_LENGTH = 4;
	public static final int INC_ALL_F_LENGTH = 4;
	public static final int SET_PRIO_F_LENGTH = 1;
	public static final int HOLD_PRIO_F_LENGTH = 1;
	public static final int FLAGS_F_LENGTH = 1;

	/*
	 * offsets of flags inside flags field in bits
	 */
	public static final int S_FLAG_OFFSET = 6;
	public static final int L_FLAG_OFFSET = 7;

	/*
	 * offsets of fields in bytes
	 */
	public static final int EXC_ANY_F_OFFSET = 0;
	public static final int INC_ANY_F_OFFSET = EXC_ANY_F_OFFSET + EXC_ANY_F_LENGTH;
	public static final int INC_ALL_F_OFFSET = INC_ANY_F_OFFSET + INC_ANY_F_LENGTH;
	public static final int SET_PRIO_F_OFFSET = INC_ALL_F_OFFSET + INC_ALL_F_LENGTH;
	public static final int HOLD_PRIO_F_OFFSET = SET_PRIO_F_OFFSET + SET_PRIO_F_LENGTH;
	public static final int FLAGS_F_OFFSET = HOLD_PRIO_F_OFFSET + HOLD_PRIO_F_LENGTH;
	public static final int TLVS_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH + 1; // added reserved field of length 1B

	public PCEPLspaObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public LspaObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null)
			throw new IllegalArgumentException("Bytes array is mandatory.");

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, FLAGS_F_OFFSET, FLAGS_F_LENGTH));

		final LspaBuilder builder = new LspaBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_F_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setHoldPriority((short) (bytes[HOLD_PRIO_F_OFFSET] & 0xFF));
		builder.setSetupPriority((short) (bytes[SET_PRIO_F_OFFSET] & 0xFF));
		builder.setLocalProtectionDesired(flags.get(L_FLAG_OFFSET));
		builder.setExcludeAny(new AttributeFilter(ByteArray.bytesToLong(ByteArray.subByte(bytes, EXC_ANY_F_OFFSET, EXC_ANY_F_LENGTH))));
		builder.setIncludeAll(new AttributeFilter(ByteArray.bytesToLong(ByteArray.subByte(bytes, INC_ALL_F_OFFSET, INC_ALL_F_LENGTH))));
		builder.setIncludeAny(new AttributeFilter(ByteArray.bytesToLong(ByteArray.subByte(bytes, INC_ANY_F_OFFSET, INC_ANY_F_LENGTH))));

		return builder.build();
	}

	@Override
	public void addTlv(final LspaBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof LspaObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed LspaObject.");

		final LspaObject lspaObj = (LspaObject) object;

		// FIXME, but no Tlvs defined
		// final byte[] tlvs = PCEPTlvParser.put(lspaObj.getTlvs());
		// final byte[] retBytes = new byte[TLVS_F_OFFSET + tlvs.length];
		// ByteArray.copyWhole(tlvs, retBytes, TLVS_F_OFFSET);

		final byte[] retBytes = new byte[TLVS_F_OFFSET];

		System.arraycopy(ByteArray.longToBytes(lspaObj.getExcludeAny().getValue()), 4, retBytes, EXC_ANY_F_OFFSET, EXC_ANY_F_LENGTH);
		System.arraycopy(ByteArray.longToBytes(lspaObj.getIncludeAny().getValue()), 4, retBytes, INC_ANY_F_OFFSET, INC_ANY_F_LENGTH);
		System.arraycopy(ByteArray.longToBytes(lspaObj.getIncludeAll().getValue()), 4, retBytes, INC_ALL_F_OFFSET, INC_ALL_F_LENGTH);
		retBytes[SET_PRIO_F_OFFSET] = ByteArray.shortToBytes(lspaObj.getSetupPriority())[Short.SIZE / Byte.SIZE - 1];
		retBytes[HOLD_PRIO_F_OFFSET] = ByteArray.shortToBytes(lspaObj.getHoldPriority())[Short.SIZE / Byte.SIZE - 1];

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(L_FLAG_OFFSET, lspaObj.isLocalProtectionDesired());
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

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
