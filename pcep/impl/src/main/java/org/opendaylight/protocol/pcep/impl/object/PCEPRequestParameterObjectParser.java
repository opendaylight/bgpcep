/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl.object;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder;

/**
 * Parser for {@link Rp}
 */
public class PCEPRequestParameterObjectParser extends AbstractObjectWithTlvsParser<RpBuilder> {

	public static final int CLASS = 2;

	public static final int TYPE = 1;

	/*
	 * lengths of fields in bytes
	 */
	private static final int FLAGS_PRI_MF_LENGTH = 4;
	private static final int RID_F_LENGTH = 4;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	private static final int FLAGS_SF_LENGTH = 29;

	/*
	 * offsets of field in bytes
	 */

	private static final int FLAGS_PRI_MF_OFFSET = 0;
	private static final int RID_F_OFFSET = FLAGS_PRI_MF_OFFSET + FLAGS_PRI_MF_LENGTH;
	private static final int TLVS_OFFSET = RID_F_OFFSET + RID_F_LENGTH;

	/*
	 * offsets of subfields inside multi-field in bits
	 */

	private static final int FLAGS_SF_OFFSET = 0;
	private static final int PRI_SF_OFFSET = FLAGS_SF_OFFSET + FLAGS_SF_LENGTH;

	/*
	 * flags offsets inside flags sub-field in bits
	 */

	private static final int O_FLAG_OFFSET = 26;
	private static final int B_FLAG_OFFSET = 27;
	private static final int R_FLAG_OFFSET = 28;

	/*
	 * GCO extension flags offsets inside flags sub-field in bits
	 */
	private static final int M_FLAG_OFFSET = 21;
	private static final int D_FLAG_OFFSET = 22;

	/*
	 * OF extension flags offsets inside flags sub.field in bits
	 */

	private static final int S_FLAG_OFFSET = 24;
	/*
	 * RFC6006 flags
	 */
	private static final int F_FLAG_OFFSET = 18;

	private static final int N_FLAG_OFFSET = 19;

	private static final int E_FLAG_OFFSET = 20;

	public PCEPRequestParameterObjectParser(final TlvHandlerRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Rp parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}

		final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(bytes, FLAGS_PRI_MF_OFFSET, FLAGS_PRI_MF_OFFSET
				+ FLAGS_PRI_MF_LENGTH));
		short priority = 0;
		priority |= flags.get(PRI_SF_OFFSET + 2) ? 1 : 0;
		priority |= (flags.get(PRI_SF_OFFSET + 1) ? 1 : 0) << 1;
		priority |= (flags.get(PRI_SF_OFFSET) ? 1 : 0) << 2;

		final RpBuilder builder = new RpBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setPriority(priority);
		builder.setFragmentation(flags.get(F_FLAG_OFFSET));
		builder.setP2mp(flags.get(N_FLAG_OFFSET));
		builder.setEroCompression(flags.get(E_FLAG_OFFSET));
		builder.setMakeBeforeBreak(flags.get(M_FLAG_OFFSET));
		builder.setOrder(flags.get(D_FLAG_OFFSET));
		builder.setSupplyOf(flags.get(S_FLAG_OFFSET));
		builder.setLoose(flags.get(O_FLAG_OFFSET));
		builder.setBiDirectional(flags.get(B_FLAG_OFFSET));
		builder.setReoptimization(flags.get(R_FLAG_OFFSET));

		builder.setRequestId(new RequestId(ByteArray.bytesToLong(Arrays.copyOfRange(bytes, RID_F_OFFSET, RID_F_OFFSET + RID_F_LENGTH))));
		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));
		return builder.build();
	}

	@Override
	public void addTlv(final RpBuilder builder, final Tlv tlv) {
		if (tlv instanceof Order) {
			builder.setTlvs(new TlvsBuilder().setOrder((Order) tlv).build());
		}
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Rp)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed RpObject.");
		}
		final Rp rPObj = (Rp) object;
		final BitSet flags = new BitSet(FLAGS_PRI_MF_LENGTH * Byte.SIZE);

		flags.set(R_FLAG_OFFSET, rPObj.isReoptimization());
		flags.set(B_FLAG_OFFSET, rPObj.isBiDirectional());
		flags.set(O_FLAG_OFFSET, rPObj.isLoose());
		flags.set(M_FLAG_OFFSET, rPObj.isMakeBeforeBreak());
		flags.set(D_FLAG_OFFSET, rPObj.isOrder());
		flags.set(S_FLAG_OFFSET, rPObj.isSupplyOf());
		flags.set(F_FLAG_OFFSET, rPObj.isFragmentation());
		flags.set(N_FLAG_OFFSET, rPObj.isP2mp());
		flags.set(E_FLAG_OFFSET, rPObj.isEroCompression());

		flags.set(PRI_SF_OFFSET, (rPObj.getPriority() & 1 << 2) != 0);
		flags.set(PRI_SF_OFFSET + 1, (rPObj.getPriority() & 1 << 1) != 0);
		flags.set(PRI_SF_OFFSET + 2, (rPObj.getPriority() & 1) != 0);

		final byte[] tlvs = serializeTlvs(rPObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length + getPadding(TLVS_OFFSET + tlvs.length, PADDED_TO)];

		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_PRI_MF_LENGTH), retBytes, FLAGS_PRI_MF_OFFSET);
		ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(rPObj.getRequestId().getValue()), (Long.SIZE / Byte.SIZE)
				- RID_F_LENGTH, RID_F_LENGTH), retBytes, RID_F_OFFSET);
		if (tlvs.length != 0) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}
		return retBytes;
	}

	public byte[] serializeTlvs(final Tlvs tlvs) {
		if (tlvs == null) {
			return new byte[0];
		} else if (tlvs.getOrder() != null) {
			return serializeTlv(tlvs.getOrder());
		}
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
