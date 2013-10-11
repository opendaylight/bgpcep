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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.no.path.tlvs.NoPathVectorBuilder;

/**
 * Parser for {@link NoPathVectorTlv}
 */
public class NoPathVectorTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 1;

	public static final int FLAGS_F_LENGTH = 4;

	/*
	 * flags offsets inside flags field in bits
	 */
	public static final int PCE_UNAVAILABLE = 31;
	public static final int UNKNOWN_DEST = 30;
	public static final int UNKNOWN_SRC = 29;

	/*
	 * flags offsets of flags added by GCO extension
	 */
	public static final int NO_GCO_SOLUTION = 25;
	public static final int NO_GCO_MIGRATION_PATH = 26;

	/*
	 * flags offsets of flags added by RFC 6006
	 */
	public static final int REACHABLITY_PROBLEM = 24;

	@Override
	public NoPathVectorTlv parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (valueBytes.length != FLAGS_F_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + "; Expected: >="
					+ FLAGS_F_LENGTH + ".");

		final BitSet flags = ByteArray.bytesToBitSet(valueBytes);

		return new NoPathVectorBuilder().setFlags(
				new Flags(false, flags.get(NO_GCO_MIGRATION_PATH), flags.get(NO_GCO_SOLUTION), flags.get(REACHABLITY_PROBLEM), false, flags.get(PCE_UNAVAILABLE), flags.get(UNKNOWN_DEST), flags.get(UNKNOWN_SRC))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlvs) {
		if (tlvs == null)
			throw new IllegalArgumentException("NoPathVectorTlv is mandatory.");
		final NoPathVectorTlv tlv = (NoPathVectorTlv) tlvs;

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);

		flags.set(PCE_UNAVAILABLE, tlv.getFlags().isPceUnavailable());
		flags.set(UNKNOWN_DEST, tlv.getFlags().isUnknownDestination());
		flags.set(UNKNOWN_SRC, tlv.getFlags().isUnknownSource());
		flags.set(NO_GCO_SOLUTION, tlv.getFlags().isNoGcoSolution());
		flags.set(NO_GCO_MIGRATION_PATH, tlv.getFlags().isNoGcoMigration());
		flags.set(REACHABLITY_PROBLEM, tlv.getFlags().isP2mpUnreachable());

		return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
