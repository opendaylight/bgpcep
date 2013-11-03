/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;

/**
 * Parser for {@link Stateful}
 */
public final class PCEStatefulCapabilityTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 16;

	private static final int FLAGS_F_LENGTH = 4;

	private static final int I_FLAG_OFFSET = 29;
	private static final int S_FLAG_OFFSET = 30;
	private static final int U_FLAG_OFFSET = 31;

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
		return new StatefulBuilder().setFlags(new Flags(flags.get(S_FLAG_OFFSET), flags.get(I_FLAG_OFFSET), flags.get(U_FLAG_OFFSET))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("StatefulCapabilityTlv is mandatory.");
		}
		final Stateful sct = (Stateful) tlv;

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(I_FLAG_OFFSET, sct.getFlags().isInitiation());
		flags.set(U_FLAG_OFFSET, sct.getFlags().isLspUpdateCapability());
		flags.set(S_FLAG_OFFSET, sct.getFlags().isIncludeDbVersion());

		return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
