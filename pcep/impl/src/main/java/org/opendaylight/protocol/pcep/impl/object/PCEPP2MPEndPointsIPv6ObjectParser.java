/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.object.PCEPP2MPEndPointsObject;
import com.google.common.primitives.UnsignedInts;

/**
 * Parser for IPv4 {@link org.opendaylight.protocol.pcep.object.PCEPP2MPEndPointsObject
 * PCEPP2MPEndPointsObject}
 */
public class PCEPP2MPEndPointsIPv6ObjectParser implements PCEPObjectParser {

    /*
     * fields lengths and offsets for IPv4 in bytes
     */
    public static final int LEAF_TYPE_F_LENGTH = 4;
    public static final int SRC6_F_LENGTH = 16;
    public static final int DEST6_F_LENGTH = 16;

    public static final int LEAF_TYPE_F_OFFSET = 0;
    public static final int SRC6_F_OFFSET = LEAF_TYPE_F_OFFSET + LEAF_TYPE_F_LENGTH;
    public static final int DEST6_F_OFFSET = SRC6_F_OFFSET + SRC6_F_LENGTH;

    @Override
    public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
	if (bytes == null)
	    throw new IllegalArgumentException("Array of bytes is mandatory");
	if (bytes.length < LEAF_TYPE_F_LENGTH + SRC6_F_LENGTH + DEST6_F_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes.");

	final long leafType = UnsignedInts.toLong(ByteArray.bytesToInt(ByteArray.subByte(bytes, LEAF_TYPE_F_OFFSET, LEAF_TYPE_F_LENGTH)));

	return new PCEPP2MPEndPointsObject<IPv6Address>(leafType, new IPv6Address(
		ByteArray.subByte(bytes, SRC6_F_OFFSET, SRC6_F_LENGTH)), Util.parseAddresses(bytes, DEST6_F_OFFSET, IPv6.FAMILY,
		DEST6_F_LENGTH), processed, ignored);
    }

    @Override
    public byte[] put(PCEPObject obj) {
	if (!(obj instanceof PCEPP2MPEndPointsObject))
	    throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPP2MPEndPointsObject.");

	final PCEPP2MPEndPointsObject<?> ePObj = (PCEPP2MPEndPointsObject<?>) obj;

	if (!(ePObj.getSourceAddress() instanceof IPv6Address))
	    throw new IllegalArgumentException("Wrong instance of NetworkAddress. Passed " + ePObj.getSourceAddress().getClass() + ". Needed IPv6Address");

	final byte[] retBytes = new byte[LEAF_TYPE_F_LENGTH + SRC6_F_LENGTH + DEST6_F_LENGTH * ePObj.getDestinationAddresses().size()];
	ByteArray.copyWhole(ByteArray.intToBytes((int) ePObj.getLeafType()), retBytes, LEAF_TYPE_F_OFFSET);
	ByteArray.copyWhole(((IPv6Address) ePObj.getSourceAddress()).getAddress(), retBytes, SRC6_F_OFFSET);
	Util.putAddresses(retBytes, DEST6_F_OFFSET, ePObj.getDestinationAddresses(), DEST6_F_LENGTH);
	return retBytes;
    }
}
