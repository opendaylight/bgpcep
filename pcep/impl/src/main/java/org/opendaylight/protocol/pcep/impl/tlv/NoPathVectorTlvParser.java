/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.BitSet;

import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;

/**
 * Parser for {@link NoPathVector}
 */
public class NoPathVectorTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 1;

	private static final int FLAGS_F_LENGTH = 4;

	private static final int REACHABLITY_PROBLEM = 24;
	private static final int NO_GCO_SOLUTION = 25;
	private static final int NO_GCO_MIGRATION_PATH = 26;
	private static final int PATH_KEY = 27;
	private static final int CHAIN_UNAVAILABLE = 28;
	private static final int UNKNOWN_SRC = 29;
	private static final int UNKNOWN_DEST = 30;
	private static final int PCE_UNAVAILABLE = 31;

	@Override
	public NoPathVector parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (valueBytes.length != FLAGS_F_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + "; Expected: >="
					+ FLAGS_F_LENGTH + ".");
		}
		final BitSet flags = ByteArray.bytesToBitSet(valueBytes);
		return new NoPathVectorBuilder().setFlags(
				new Flags(flags.get(CHAIN_UNAVAILABLE), flags.get(NO_GCO_MIGRATION_PATH), flags.get(NO_GCO_SOLUTION), flags.get(REACHABLITY_PROBLEM), flags.get(PATH_KEY), flags.get(PCE_UNAVAILABLE), flags.get(UNKNOWN_DEST), flags.get(UNKNOWN_SRC))).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlvs) {
		if (tlvs == null) {
			throw new IllegalArgumentException("NoPathVectorTlv is mandatory.");
		}
		final NoPathVector tlv = (NoPathVector) tlvs;

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		flags.set(REACHABLITY_PROBLEM, tlv.getFlags().isP2mpUnreachable());
		flags.set(NO_GCO_SOLUTION, tlv.getFlags().isNoGcoSolution());
		flags.set(NO_GCO_MIGRATION_PATH, tlv.getFlags().isNoGcoMigration());
		flags.set(PATH_KEY, tlv.getFlags().isPathKey());
		flags.set(CHAIN_UNAVAILABLE, tlv.getFlags().isChainUnavailable());
		flags.set(UNKNOWN_SRC, tlv.getFlags().isUnknownSource());
		flags.set(UNKNOWN_DEST, tlv.getFlags().isUnknownDestination());
		flags.set(PCE_UNAVAILABLE, tlv.getFlags().isPceUnavailable());
		return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
