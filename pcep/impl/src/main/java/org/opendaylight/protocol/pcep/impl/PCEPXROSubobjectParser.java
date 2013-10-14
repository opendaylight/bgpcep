/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIPv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIPv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSRLGSubobject;
import org.opendaylight.protocol.pcep.subobject.XROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.PCEPXROSubobject PCEPXROSubobject}
 */
public class PCEPXROSubobjectParser {

	private static final Logger logger = LoggerFactory.getLogger(PCEPXROSubobjectParser.class);

	/**
	 * Type identifier for {@link org.opendaylight.protocol.pcep.PCEPXROSubobject PCEPXROSubobject}
	 */
	public enum PCEPXROSubobjectType {
		IPv4_PREFIX(1), IPv6_PREFIX(2), UNNUMBERED_INTERFACE_ID(4), AS_NUMBER(32), SRLG(34);

		private final int indicator;

		PCEPXROSubobjectType(final int indicator) {
			this.indicator = indicator;
		}

		public int getIndicator() {
			return this.indicator;
		}

		public static PCEPXROSubobjectType getFromInt(final int type) throws PCEPDeserializerException {

			for (final PCEPXROSubobjectType type_e : PCEPXROSubobjectType.values()) {
				if (type_e.getIndicator() == type)
					return type_e;
			}

			throw new PCEPDeserializerException("Unknown Subobject type. Passed: " + type + "; Known: " + PCEPXROSubobjectType.values()
					+ ".");
		}
	}

	/*
	 * Fields lengths in Bytes
	 */
	public static final int TYPE_FLAG_F_LENGTH = 1;
	public static final int LENGTH_F_LENGTH = 1;

	/*
	 * Fields offsets in Bytes
	 */
	public static final int TYPE_FLAG_F_OFFSET = 0;
	public static final int LENGTH_F_OFFSET = TYPE_FLAG_F_OFFSET + TYPE_FLAG_F_LENGTH;
	public static final int SO_CONTENTS_OFFSET = LENGTH_F_OFFSET + LENGTH_F_LENGTH;

	public static List<ExcludeRouteSubobject> parse(final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		final List<ExcludeRouteSubobject> subobjsList = new ArrayList<ExcludeRouteSubobject>();
		boolean mandatoryFlag;
		PCEPXROSubobjectType type;
		byte[] soContentsBytes;
		int length;
		int offset = 0;

		while (offset < bytes.length) {

			mandatoryFlag = ((bytes[offset + TYPE_FLAG_F_OFFSET] & (1 << 7)) != 0);
			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, LENGTH_F_LENGTH));

			type = PCEPXROSubobjectType.getFromInt((bytes[offset + TYPE_FLAG_F_OFFSET] & 0xff) & ~(1 << 7));

			if (length > bytes.length - offset)
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ (bytes.length - offset));

			soContentsBytes = new byte[length - SO_CONTENTS_OFFSET];
			System.arraycopy(bytes, offset + SO_CONTENTS_OFFSET, soContentsBytes, 0, length - SO_CONTENTS_OFFSET);

			logger.debug("Attempt to parse exclude route objects subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
			final ExcludeRouteSubobject subObj = parseSpecificSubobject(type, soContentsBytes, mandatoryFlag);
			logger.debug("Subobject was parsed. {}", subObj);

			subobjsList.add(subObj);

			offset += length;
		}

		return subobjsList;
	}

	public static byte[] put(final List<ExcludeRouteSubobject> objsToSerialize) {
		final List<byte[]> bytesList = new ArrayList<byte[]>(objsToSerialize.size());

		int length = 0;
		for (final ExcludeRouteSubobject obj : objsToSerialize) {
			final byte[] bytes = put(obj);
			length += bytes.length;
			bytesList.add(bytes);
		}

		final byte[] retBytes = new byte[length];

		int offset = 0;
		for (final byte[] bytes : bytesList) {
			System.arraycopy(bytes, 0, retBytes, offset, bytes.length);
			offset += bytes.length;
		}

		return retBytes;
	}

	public static byte[] put(final ExcludeRouteSubobject objToSerialize) {
		int typeIndicator = 0;

		final byte[] soContentsBytes;

		if (objToSerialize instanceof XROIPPrefixSubobject && ((XROIPPrefixSubobject) objToSerialize).getPrefix().getIpv4Prefix() != null) {
			typeIndicator = PCEPXROSubobjectType.IPv4_PREFIX.getIndicator();
			soContentsBytes = XROIPv4PrefixSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof XROIPPrefixSubobject
				&& ((XROIPPrefixSubobject) objToSerialize).getPrefix().getIpv6Prefix() != null) {
			typeIndicator = PCEPXROSubobjectType.IPv6_PREFIX.getIndicator();
			soContentsBytes = XROIPv6PrefixSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof XROAsNumberSubobject) {
			typeIndicator = PCEPXROSubobjectType.AS_NUMBER.getIndicator();
			soContentsBytes = XROAsNumberSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof XROUnnumberedInterfaceSubobject) {
			typeIndicator = PCEPXROSubobjectType.UNNUMBERED_INTERFACE_ID.getIndicator();
			soContentsBytes = XROUnnumberedInterfaceSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof XROSRLGSubobject) {
			typeIndicator = PCEPXROSubobjectType.SRLG.getIndicator();
			soContentsBytes = XROSRLGSubobjectParser.put(objToSerialize);
		} else
			throw new IllegalArgumentException("Unknown instance of PCEPXROSubobject. Passed: " + objToSerialize.getClass() + ".");

		final byte[] bytes = new byte[SO_CONTENTS_OFFSET + soContentsBytes.length];

		bytes[TYPE_FLAG_F_OFFSET] = (byte) (ByteArray.cutBytes(ByteArray.intToBytes(typeIndicator), (Integer.SIZE / 8) - TYPE_FLAG_F_LENGTH)[0] | (objToSerialize.isMandatory() ? 1 << 7
				: 0));
		bytes[LENGTH_F_OFFSET] = ByteArray.cutBytes(ByteArray.intToBytes(soContentsBytes.length + SO_CONTENTS_OFFSET), (Integer.SIZE / 8)
				- LENGTH_F_LENGTH)[0];

		System.arraycopy(soContentsBytes, 0, bytes, SO_CONTENTS_OFFSET, soContentsBytes.length);

		return bytes;
	}

	private static ExcludeRouteSubobject parseSpecificSubobject(final PCEPXROSubobjectType type, final byte[] soContentsBytes,
			final boolean mandatory) throws PCEPDeserializerException {

		switch (type) {
		case IPv4_PREFIX:
			return XROIPv4PrefixSubobjectParser.parse(soContentsBytes, mandatory);
		case IPv6_PREFIX:
			return XROIPv6PrefixSubobjectParser.parse(soContentsBytes, mandatory);
		case UNNUMBERED_INTERFACE_ID:
			return XROUnnumberedInterfaceSubobjectParser.parse(soContentsBytes, mandatory);
		case AS_NUMBER:
			return XROAsNumberSubobjectParser.parse(soContentsBytes, mandatory);
		case SRLG:
			return XROSRLGSubobjectParser.parse(soContentsBytes, mandatory);
		default:
			throw new PCEPDeserializerException("Unknown Subobject type. Passed: " + type + ".");
		}
	}
}
