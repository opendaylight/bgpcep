/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public class Stateful02StatefulCapabilityTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 16;

	protected static final int FLAGS_F_LENGTH = 4;

	protected static final int S_FLAG_OFFSET = 30;
	protected static final int U_FLAG_OFFSET = 31;

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
		return sb.build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("StatefulCapabilityTlv is mandatory.");
		}
		final Stateful sct = (Stateful) tlv;
		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(U_FLAG_OFFSET, sct.isLspUpdateCapability());
		flags.set(S_FLAG_OFFSET, sct.isIncludeDbVersion());
		return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
