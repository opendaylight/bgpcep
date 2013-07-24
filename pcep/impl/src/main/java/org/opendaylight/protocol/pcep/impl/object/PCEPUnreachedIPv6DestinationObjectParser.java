/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.Util;
import org.opendaylight.protocol.pcep.object.PCEPUnreachedDestinationObject;

public class PCEPUnreachedIPv6DestinationObjectParser implements PCEPObjectParser {

    /*
     * fields lengths and offsets for IPv6 in bytes
     */
    public static final int DEST6_F_LENGTH = 16;

    public static final int DEST6_F_OFFSET = 0;

    @Override
    public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
	if (bytes == null)
	    throw new IllegalArgumentException("Array of bytes is mandatory");

	return new PCEPUnreachedDestinationObject<IPv6Address>(Util.parseAddresses(bytes, DEST6_F_OFFSET, IPv6.FAMILY, DEST6_F_LENGTH), processed, ignored);
    }

    @Override
    public byte[] put(PCEPObject obj) {
	if (!(obj instanceof PCEPUnreachedDestinationObject))
	    throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPUnreachedDestinationObject.");

	final PCEPUnreachedDestinationObject<?> uDObj = (PCEPUnreachedDestinationObject<?>) obj;

	if (uDObj.getUnreachedDestinations().isEmpty())
	    return new byte[0];

	if (!(uDObj.getUnreachedDestinations().get(0) instanceof IPv6Address))
	    throw new IllegalArgumentException("Wrong instance of NetworkAddress. Passed " + uDObj.getUnreachedDestinations().get(0).getClass()
		    + ". Needed IPv6Address");

	final byte[] retBytes = new byte[DEST6_F_LENGTH * uDObj.getUnreachedDestinations().size()];
	Util.putAddresses(retBytes, DEST6_F_OFFSET, uDObj.getUnreachedDestinations(), DEST6_F_LENGTH);
	return retBytes;
    }
}
