/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.BitSet;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.PCEPSvecObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SvecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder;

import com.google.common.collect.Lists;

/**
 * Parser for {@link SvecObject}
 */
public class PCEPSvecObjectParser extends AbstractObjectParser<SvecBuilder> {

	/*
	 * field lengths in bytes
	 */
	public static final int FLAGS_F_LENGTH = 3;
	public static final int REQ_LIST_ITEM_LENGTH = 4;

	/*
	 * fields offsets in bytes
	 */
	public static final int FLAGS_F_OFFSET = 1; // aded reserved field of size 1
	public static final int REQ_ID_LIST_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

	/*
	 * flags offsets inside flags field in bits
	 */
	public static final int S_FLAG_OFFSET = 21;
	public static final int N_FLAG_OFFSET = 22;
	public static final int L_FLAG_OFFSET = 23;

	public static final int P_FLAG_OFFSET = 19;
	public static final int D_FLAG_OFFSET = 20;

	/*
	 * min size in bytes
	 */

	public static final int MIN_SIZE = FLAGS_F_LENGTH + FLAGS_F_OFFSET;

	public PCEPSvecObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public SvecObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length < MIN_SIZE)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + MIN_SIZE
					+ ".");

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, FLAGS_F_OFFSET, FLAGS_F_LENGTH));
		final List<RequestId> requestIDs = Lists.newArrayList();

		for (int i = REQ_ID_LIST_OFFSET; i < bytes.length; i += REQ_LIST_ITEM_LENGTH) {
			requestIDs.add(new RequestId(ByteArray.bytesToLong(ByteArray.subByte(bytes, i, REQ_LIST_ITEM_LENGTH))));
		}

		if (requestIDs.isEmpty())
			throw new PCEPDeserializerException("Empty Svec Object - no request ids.");

		final SvecBuilder builder = new SvecBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		builder.setLinkDiverse(flags.get(L_FLAG_OFFSET));
		builder.setNodeDiverse(flags.get(N_FLAG_OFFSET));
		builder.setSrlgDiverse(flags.get(S_FLAG_OFFSET));
		builder.setRequestsIds(requestIDs);

		return builder.build();
	}

	@Override
	public void addTlv(final SvecBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	// return new PCEPSvecObject(flags.get(P_FLAG_OFFSET), flags.get(D_FLAG_OFFSET));

	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPSvecObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPSvecObject.");

		final PCEPSvecObject svecObj = (PCEPSvecObject) obj;
		final byte[] retBytes = new byte[svecObj.getRequestIDs().size() * REQ_LIST_ITEM_LENGTH + REQ_ID_LIST_OFFSET];
		final List<Long> requestIDs = svecObj.getRequestIDs();
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(L_FLAG_OFFSET, svecObj.isLinkDiversed());
		flags.set(N_FLAG_OFFSET, svecObj.isNodeDiversed());
		flags.set(S_FLAG_OFFSET, svecObj.isSrlgDiversed());
		flags.set(P_FLAG_OFFSET, svecObj.isParitialPathDiversed());
		flags.set(D_FLAG_OFFSET, svecObj.isLinkDirectionDiversed());

		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

		for (int i = 0; i < requestIDs.size(); i++) {
			System.arraycopy(ByteArray.longToBytes(requestIDs.get(i)), 4, retBytes, REQ_LIST_ITEM_LENGTH * i + REQ_ID_LIST_OFFSET,
					REQ_LIST_ITEM_LENGTH);
		}

		assert !(requestIDs.isEmpty()) : "Empty Svec Object - no request ids.";

		return retBytes;
	}
}
