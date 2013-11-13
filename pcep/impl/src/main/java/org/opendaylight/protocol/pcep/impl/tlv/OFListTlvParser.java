/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.PCEPOFCodesMapping;
import org.opendaylight.protocol.pcep.impl.PCEPTlvParser;
import org.opendaylight.protocol.pcep.tlv.OFListTlv;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.tlv.OFListTlv OFListTlv}
 */
public class OFListTlvParser implements PCEPTlvParser {

	private static final int OF_CODE_ELEMENT_LENGTH = 2;
	
	public static final int TYPE = 4;

	public OFListTlv parse(byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		if (valueBytes.length % OF_CODE_ELEMENT_LENGTH != 0)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + ".");

		final List<PCEPOFCodes> ofCodes = new ArrayList<PCEPOFCodes>();
		for (int i = 0; i < valueBytes.length; i += OF_CODE_ELEMENT_LENGTH) {
			try {
				ofCodes.add(PCEPOFCodesMapping.getInstance().getFromCodeIdentifier(
						ByteArray.bytesToShort(Arrays.copyOfRange(valueBytes, i, i + OF_CODE_ELEMENT_LENGTH)) & 0xFFFF));
			} catch (final NoSuchElementException nsee) {
				throw new PCEPDeserializerException(nsee, "Unknown OF Code inside OF Code list Tlv.");
			}
		}

		return new OFListTlv(ofCodes);
	}

	public byte[] put(PCEPTlv objToSerialize) {
		if (objToSerialize == null)
			throw new IllegalArgumentException("OFListTlv is mandatory.");

		OFListTlv tlv = (OFListTlv) objToSerialize;
		
		final List<PCEPOFCodes> ofCodes = tlv.getOfCodes();
		final byte[] retBytes = new byte[ofCodes.size() * OF_CODE_ELEMENT_LENGTH];

		final int size = ofCodes.size();
		for (int i = 0; i < size; i++) {
			ByteArray.copyWhole(ByteArray.shortToBytes((short) PCEPOFCodesMapping.getInstance().getFromOFCodesEnum(ofCodes.get(i))), retBytes, i
					* OF_CODE_ELEMENT_LENGTH);
		}

		return retBytes;
	}
}
