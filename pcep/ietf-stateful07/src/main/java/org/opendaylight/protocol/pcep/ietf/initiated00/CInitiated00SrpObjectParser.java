/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07SrpObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;

/**
 * Parser for {@link Srp}
 */
public final class CInitiated00SrpObjectParser extends Stateful07SrpObjectParser {

	private static final int REMOVE_FLAG = 31;

	public CInitiated00SrpObjectParser(final TlvRegistry tlvReg) {
		super(tlvReg);
	}

	@Override
	public Srp parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (bytes.length < MIN_SIZE) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + MIN_SIZE
					+ ".");
		}
		if (header.isProcessingRule()) {
			throw new PCEPDeserializerException("Processed flag is set");
		}
		final SrpBuilder builder = new SrpBuilder();
		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(bytes, 0, FLAGS_SIZE));
		builder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(flags.get(REMOVE_FLAG)).build());
		final byte[] srpId = ByteArray.subByte(bytes, FLAGS_SIZE, SRP_ID_SIZE);
		if (Arrays.equals(srpId, new byte[] { 0, 0, 0, 0 }) || Arrays.equals(srpId, new byte[] { 0xFFFFFFFF })) {
			throw new PCEPDeserializerException("Min/Max values for SRP ID are reserved.");
		}
		builder.setOperationId(new SrpIdNumber(ByteArray.bytesToLong(srpId)));
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Srp)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed SrpObject.");
		}
		final Srp srp = (Srp) object;
		final byte[] tlvs = serializeTlvs(srp.getTlvs());
		final Long id = srp.getOperationId().getValue();
		if (id == 0 || id == 0xFFFFFFFFL) {
			throw new IllegalArgumentException("Min/Max values for SRP ID are reserved.");
		}
		final byte[] retBytes = new byte[MIN_SIZE];
		if (tlvs != null) {
			ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		}
		final BitSet flags = new BitSet(FLAGS_SIZE * Byte.SIZE);
		flags.set(REMOVE_FLAG, srp.getAugmentation(Srp1.class).isRemove());
		ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_SIZE), retBytes, 0);

		System.arraycopy(ByteArray.intToBytes(id.intValue(), SRP_ID_SIZE), 0, retBytes, FLAGS_SIZE, SRP_ID_SIZE);
		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);
		return ObjectUtil.formatSubobject(TYPE, CLASS, object.isProcessingRule(), object.isIgnore(), retBytes);
	}
}
