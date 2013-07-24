/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.concepts.Bandwidth;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject
 * PCEPLoadBalancingObject}
 */
public class PCEPLoadBalancingObjectParser implements PCEPObjectParser {

	public static final int FLAGS_F_LENGTH = 1;
	public static final int MAX_LSP_F_LENGTH = 1;
	public static final int MIN_BAND_F_LENGTH = 4;

	public static final int FLAGS_F_OFFSET = 2;
	public static final int MAX_LSP_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int MIN_BAND_F_OFFSET = MAX_LSP_F_OFFSET + MAX_LSP_F_LENGTH;

	public static final int SIZE = MIN_BAND_F_OFFSET + MIN_BAND_F_LENGTH;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		if (bytes.length != SIZE)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: " + SIZE + ".");

		return new PCEPLoadBalancingObject(bytes[MAX_LSP_F_OFFSET] & 0xFF, new Bandwidth(ByteArray.bytesToFloat(ByteArray.subByte(bytes, MIN_BAND_F_OFFSET,
				MIN_BAND_F_LENGTH))), processed);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPLoadBalancingObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPLoadBalancingObject.");

		final PCEPLoadBalancingObject specObj = (PCEPLoadBalancingObject) obj;

		final byte[] retBytes = new byte[SIZE];

		retBytes[MAX_LSP_F_OFFSET] = ByteArray.intToBytes(specObj.getMaxLSP())[Integer.SIZE / Byte.SIZE - 1];
		ByteArray.copyWhole(ByteArray.floatToBytes((float) specObj.getMinBandwidth().getBytesPerSecond()), retBytes, MIN_BAND_F_OFFSET);

		return retBytes;
	}

}
