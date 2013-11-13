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
import org.opendaylight.protocol.pcep.impl.PCEPTlvFactory;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPNotificationObject
 * PCEPNotificationObject}
 */
public class PCEPNotificationObjectParser implements PCEPObjectParser {

	/*
	 * lengths of fields
	 */
	public static final int FLAGS_F_LENGTH = 1;
	public static final int NT_F_LENGTH = 1;
	public static final int NV_F_LENGTH = 1;

	/*
	 * offsets of fields
	 */
	public static final int FLAGS_F_OFFSET = 1; //added reserved filed of size 1
	public static final int NT_F_OFFSET = FLAGS_F_OFFSET + FLAGS_F_LENGTH;
	public static final int NV_F_OFFSET = NT_F_OFFSET + NT_F_LENGTH;
	public static final int TLVS_OFFSET = NV_F_OFFSET + NV_F_LENGTH;

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (bytes.length < TLVS_OFFSET)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + bytes.length + "; Expected: >=" + TLVS_OFFSET + ".");

		return new PCEPNotificationObject((short) (bytes[NT_F_OFFSET] & 0xFF), (short) (bytes[NV_F_OFFSET] & 0xFF), PCEPTlvFactory.parse(ByteArray.cutBytes(
				bytes, TLVS_OFFSET)));
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPNotificationObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPNotificationObject.");

		final PCEPNotificationObject notObj = (PCEPNotificationObject) obj;

		final byte[] tlvs = PCEPTlvFactory.put(notObj.getTlvs());
		final byte[] retBytes = new byte[TLVS_OFFSET + tlvs.length];

		ByteArray.copyWhole(tlvs, retBytes, TLVS_OFFSET);

		retBytes[NT_F_OFFSET] = ByteArray.shortToBytes(notObj.getType())[1];
		retBytes[NV_F_OFFSET] = ByteArray.shortToBytes(notObj.getValue())[1];

		return retBytes;
	}

}
