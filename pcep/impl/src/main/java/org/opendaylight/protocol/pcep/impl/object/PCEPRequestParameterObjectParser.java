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
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.tlvs.OrderBuilder;

/**
 * Parser for {@link RpObject}
 */

public class PCEPRequestParameterObjectParser extends AbstractObjectParser<RpBuilder> {

	/*
	 * lengths of fields in bytes
	 */
	public static final int FLAGS_PRI_MF_LENGTH = 4; // multi-field
	public static final int RID_F_LENGTH = 4;

	/*
	 * lengths of subfields inside multi-field in bits
	 */
	public static final int FLAGS_SF_LENGTH = 29;
	public static final int PRI_SF_LENGTH = 3;

	/*
	 * offsets of field in bytes
	 */

	public static final int FLAGS_PRI_MF_OFFSET = 0;
	public static final int RID_F_OFFSET = FLAGS_PRI_MF_OFFSET + FLAGS_PRI_MF_LENGTH;
	public static final int TLVS_OFFSET = RID_F_OFFSET + RID_F_LENGTH;

	/*
	 * offsets of subfields inside multi-field in bits
	 */

	public static final int FLAGS_SF_OFFSET = 0;
	public static final int PRI_SF_OFFSET = FLAGS_SF_OFFSET + FLAGS_SF_LENGTH;

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

	private static int S_FLAG_OFFSET = 24; // Supply OF on response

	/*
	 * RFC6006 flags
	 */
	private static int F_FLAG_OFFSET = 18;

	private static int N_FLAG_OFFSET = 19;

	private static int E_FLAG_OFFSET = 20;

	public PCEPRequestParameterObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public RpObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(bytes, FLAGS_PRI_MF_OFFSET, FLAGS_PRI_MF_OFFSET
				+ FLAGS_PRI_MF_LENGTH));
		short priority = 0;
		priority |= flags.get(PRI_SF_OFFSET + 2) ? 1 : 0;
		priority |= (flags.get(PRI_SF_OFFSET + 1) ? 1 : 0) << 1;
		priority |= (flags.get(PRI_SF_OFFSET) ? 1 : 0) << 2;

		final RpBuilder builder = new RpBuilder();

		parseTlvs(builder, ByteArray.cutBytes(bytes, TLVS_OFFSET));

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setBiDirectional(flags.get(B_FLAG_OFFSET));
		builder.setEroCompression(flags.get(E_FLAG_OFFSET));
		builder.setFragmentation(flags.get(F_FLAG_OFFSET));
		builder.setLoose(flags.get(O_FLAG_OFFSET));
		builder.setMakeBeforeBreak(flags.get(M_FLAG_OFFSET));
		builder.setOrder(flags.get(D_FLAG_OFFSET));
		builder.setP2mp(flags.get(N_FLAG_OFFSET));
		builder.setReoptimization(flags.get(R_FLAG_OFFSET));
		builder.setSupplyOf(flags.get(S_FLAG_OFFSET));
		builder.setPriority(priority);
		builder.setRequestId(new RequestId(ByteArray.bytesToLong(Arrays.copyOfRange(bytes, RID_F_OFFSET, RID_F_OFFSET + RID_F_LENGTH))));

		return builder.build();
	}

	@Override
	public void addTlv(final RpBuilder builder, final Tlv tlv) {
		final TlvsBuilder tbuilder = new TlvsBuilder();
		if (tlv instanceof OrderTlv) {
			final OrderBuilder b = new OrderBuilder();
			b.setDelete(((OrderTlv) tlv).getDelete());
			b.setSetup(((OrderTlv) tlv).getSetup());
			tbuilder.setOrder(b.build());
		}
		builder.setTlvs(tbuilder.build());
	}

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPRequestParameterObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass()
					+ ". Needed PCEPRequestParameterObject.");

		final PCEPRequestParameterObject rPObj = (PCEPRequestParameterObject) obj;

		final BitSet flags_priority = new BitSet(FLAGS_PRI_MF_LENGTH * Byte.SIZE);

		flags_priority.set(R_FLAG_OFFSET, rPObj.isReoptimized());
		flags_priority.set(B_FLAG_OFFSET, rPObj.isBidirectional());
		flags_priority.set(O_FLAG_OFFSET, rPObj.isLoose());
		flags_priority.set(M_FLAG_OFFSET, rPObj.isMakeBeforeBreak());
		flags_priority.set(D_FLAG_OFFSET, rPObj.isReportRequestOrder());
		flags_priority.set(S_FLAG_OFFSET, rPObj.isSuplyOFOnResponse());
		flags_priority.set(F_FLAG_OFFSET, rPObj.isFragmentation());
		flags_priority.set(N_FLAG_OFFSET, rPObj.isP2mp());
		flags_priority.set(E_FLAG_OFFSET, rPObj.isEroCompression());

		flags_priority.set(PRI_SF_OFFSET, (rPObj.getPriority() & 1 << 2) != 0);
		flags_priority.set(PRI_SF_OFFSET + 1, (rPObj.getPriority() & 1 << 1) != 0);
		flags_priority.set(PRI_SF_OFFSET + 2, (rPObj.getPriority() & 1) != 0);

		final byte[] tlvs = PCEPTlvParser.put(rPObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length];
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags_priority, FLAGS_PRI_MF_LENGTH), retBytes, FLAGS_PRI_MF_OFFSET);
		ByteArray.copyWhole(
				ByteArray.subByte(ByteArray.longToBytes(rPObj.getRequestID()), (Long.SIZE / Byte.SIZE) - RID_F_LENGTH, RID_F_LENGTH),
				retBytes, RID_F_OFFSET);

		return retBytes;
	}
}
