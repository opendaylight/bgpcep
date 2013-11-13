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
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.tlv.NoPathVectorTlv;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.tlv.NoPathVectorTlv NoPathVectorTlv}
 */
public class NoPathVectorTlvParser implements PCEPTlvParser {
	
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

	public NoPathVectorTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (valueBytes.length != FLAGS_F_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + "; Expected: >=" + FLAGS_F_LENGTH + ".");

		final BitSet flags = ByteArray.bytesToBitSet(valueBytes);
		return new NoPathVectorTlv(flags.get(PCE_UNAVAILABLE), flags.get(UNKNOWN_DEST), flags.get(UNKNOWN_SRC), flags.get(NO_GCO_SOLUTION),
				flags.get(NO_GCO_MIGRATION_PATH), flags.get(REACHABLITY_PROBLEM));
	}

	public byte[] put(PCEPTlv tlv) {
		if (tlv == null)
			throw new IllegalArgumentException("NoPathVectorTlv is mandatory.");

		final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);
		
		NoPathVectorTlv obj = (NoPathVectorTlv) tlv;

		flags.set(PCE_UNAVAILABLE, obj.isPceUnavailable());
		flags.set(UNKNOWN_DEST, obj.isUnknownDest());
		flags.set(UNKNOWN_SRC, obj.isUnknownSrc());
		flags.set(NO_GCO_SOLUTION, obj.isNoGCOSolution());
		flags.set(NO_GCO_MIGRATION_PATH, obj.isNoGCOMigrationPath());
		flags.set(REACHABLITY_PROBLEM, obj.isReachablityProblem());

		return ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH);
	}
}
