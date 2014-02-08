/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.crabbe.initiated00;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02StatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

/**
 * Parser for {@link Stateful}
 */
public final class PCEStatefulCapabilityTlvParser extends Stateful02StatefulCapabilityTlvParser {

	private static final int I_FLAG_OFFSET = 29;

	@Override
	public Stateful parseTlv(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		}
		if (buffer.length < FLAGS_F_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: >= "
					+ FLAGS_F_LENGTH + ".");
		}

		final BitSet flags = ByteArray.bytesToBitSet(ByteArray.subByte(buffer, 0, FLAGS_F_LENGTH));

		final StatefulBuilder sb = new StatefulBuilder();
		sb.setIncludeDbVersion(flags.get(S_FLAG_OFFSET));
		sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));

		if (flags.get(I_FLAG_OFFSET)) {
			sb.addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(Boolean.TRUE).build());
		}

		return sb.build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("StatefulCapabilityTlv is mandatory.");
		}
		final Stateful sct = (Stateful) tlv;

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);

		final Stateful1 sfi = sct.getAugmentation(Stateful1.class);
		if (sfi != null) {
			flags.set(I_FLAG_OFFSET, sfi.isInitiation());
		}
		if (sct.isLspUpdateCapability() != null && sct.isLspUpdateCapability()) {
			flags.set(U_FLAG_OFFSET, sct.isLspUpdateCapability());
		}
		if (sct.isIncludeDbVersion() != null && sct.isIncludeDbVersion()) {
			flags.set(S_FLAG_OFFSET, sct.isIncludeDbVersion());
		}
		return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
	}
}