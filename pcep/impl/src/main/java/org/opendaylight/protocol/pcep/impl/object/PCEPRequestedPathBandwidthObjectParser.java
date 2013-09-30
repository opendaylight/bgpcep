/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject
 * PCEPRequestedPathBandwidthObject}
 */
public class PCEPRequestedPathBandwidthObjectParser implements PCEPObjectParser {

	private static final int BANDWIDTH_F_LENGTH = 4;

	@Override
	public PCEPObject parse(final byte[] bytes, final boolean processed, final boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (bytes.length != BANDWIDTH_F_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: "
					+ BANDWIDTH_F_LENGTH + ".");

		return new PCEPRequestedPathBandwidthObject(new Bandwidth(bytes), processed, ignored);
	}

	@Override
	public byte[] put(final PCEPObject obj) {
		if (!(obj instanceof PCEPRequestedPathBandwidthObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass()
					+ ". Needed PCEPRequestedPathBandwidthObject.");

		return ((PCEPRequestedPathBandwidthObject) obj).getBandwidth().getValue();
	}
}
