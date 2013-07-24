/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject
 * RROIPAddressSubobject<IPv6Prefix>}
 */
public class RROIPv6AddressSubobjectParser {
    public static final int IP_F_LENGTH = 16;
    public static final int PREFIX_F_LENGTH = 1;
    public static final int FLAGS_F_LENGTH = 1;

    public static final int IP_F_OFFSET = 0;
    public static final int PREFIX_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;
    public static final int FLAGS_F_OFFSET = PREFIX_F_OFFSET + PREFIX_F_LENGTH;

    public static final int CONTENT_LENGTH = FLAGS_F_OFFSET + FLAGS_F_LENGTH;

    /*
     * flags offset in bits
     */
    public static final int LPA_F_OFFSET = 7;
    public static final int LPIU_F_OFFSET = 6;

    public static RROIPAddressSubobject<IPv6Prefix> parse(byte[] soContentsBytes) throws PCEPDeserializerException {
	if (soContentsBytes == null || soContentsBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (soContentsBytes.length != CONTENT_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: " + CONTENT_LENGTH + ".");

	final IPv6Address address = new IPv6Address(ByteArray.subByte(soContentsBytes, IP_F_OFFSET, IP_F_LENGTH));
	final int length = UnsignedBytes.toInt(soContentsBytes[PREFIX_F_OFFSET]);

	final BitSet flags = ByteArray.bytesToBitSet(Arrays.copyOfRange(soContentsBytes, FLAGS_F_OFFSET, FLAGS_F_OFFSET + FLAGS_F_LENGTH));

	return new RROIPAddressSubobject<IPv6Prefix>(new IPv6Prefix(address, length), flags.get(LPA_F_OFFSET), flags.get(LPIU_F_OFFSET));
    }

    public static byte[] put(ReportedRouteSubobject objToSerialize) {
	if (!(objToSerialize instanceof RROIPAddressSubobject))
	    throw new IllegalArgumentException("Unknown ReportedRouteSubobject instance. Passed " + objToSerialize.getClass()
		    + ". Needed RROIPAddressSubobject.");

	final RROIPAddressSubobject<?> specObj = (RROIPAddressSubobject<?>) objToSerialize;
	final Prefix<?> prefix = specObj.getPrefix();

	if (!(prefix instanceof IPv6Prefix))
	    throw new IllegalArgumentException("Unknown AbstractPrefix instance. Passed " + prefix.getClass() + ". Needed IPv6Prefix.");

	final BitSet flags = new BitSet(FLAGS_F_LENGTH * Byte.SIZE);

	flags.set(LPA_F_OFFSET, specObj.isLocalProtectionAvailable());
	flags.set(LPIU_F_OFFSET, specObj.isLocalProtectionInUse());

	final byte[] retBytes = new byte[CONTENT_LENGTH];
	ByteArray.copyWhole(prefix.getAddress().getAddress(), retBytes, IP_F_OFFSET);
	retBytes[PREFIX_F_OFFSET] = ByteArray.intToBytes(prefix.getLength())[Integer.SIZE / Byte.SIZE - 1];
	ByteArray.copyWhole(ByteArray.bitSetToBytes(flags, FLAGS_F_LENGTH), retBytes, FLAGS_F_OFFSET);

	return retBytes;
    }
}
