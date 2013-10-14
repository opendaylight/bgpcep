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

import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.RROAttributesSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROIPv4AddressSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROIPv6AddressSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROPathKeyWith128PCEIDSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROPathKeyWith32PCEIDSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.subobject.RROAttributesSubobject;
import org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject;
import org.opendaylight.protocol.pcep.subobject.RROLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.RROPathKeyWith128PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.RROPathKeyWith32PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.RROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.PCEPSubobject PCEPSubobject}
 */
public class PCEPRROSubobjectParser {

	private static final Logger logger = LoggerFactory.getLogger(PCEPRROSubobjectParser.class);

	/**
	 * Type identifier for {@link org.opendaylight.protocol.pcep.PCEPSubobject PCEPSubobject}
	 */
	public enum PCEPSubobjectType {
		IPv4_PREFIX(1), IPv6_PREFIX(2), LABEL(3), UNNUMBERED_INTERFACE_ID(4), ATTRIBUTES(5), PROTECTION(37), PK_32(64), PK_128(65);

		private final int indicator;

		PCEPSubobjectType(final int indicator) {
			this.indicator = indicator;
		}

		public int getIndicator() {
			return this.indicator;
		}

		public static PCEPSubobjectType getFromInt(final int type) throws PCEPDeserializerException {

			for (final PCEPSubobjectType type_e : PCEPSubobjectType.values()) {
				if (type_e.getIndicator() == type)
					return type_e;
			}

			throw new PCEPDeserializerException("Unknown Subobject type. Passed: " + type + "; Known: " + PCEPSubobjectType.values() + ".");
		}
	}

	/*
	 * Fields lengths in Bytes
	 */
	public static final int TYPE_F_LENGTH = 1;
	public static final int LENGTH_F_LENGTH = 1;

	/*
	 * Fields offsets in Bytes
	 */
	public static final int TYPE_F_OFFSET = 0;
	public static final int LENGTH_F_OFFSET = TYPE_F_OFFSET + TYPE_F_LENGTH;
	public static final int SO_CONTENTS_OFFSET = LENGTH_F_OFFSET + LENGTH_F_LENGTH;

	public static List<ReportedRouteSubobject> parse(final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		final List<ReportedRouteSubobject> subobjsList = new ArrayList<ReportedRouteSubobject>();
		PCEPSubobjectType type;
		byte[] soContentsBytes;
		int length;
		int offset = 0;

		while (offset < bytes.length) {
			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, LENGTH_F_LENGTH));

			type = PCEPSubobjectType.getFromInt(bytes[offset + TYPE_F_OFFSET] & 0xff);

			if (length > bytes.length - offset)
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ (bytes.length - offset));

			soContentsBytes = new byte[length - SO_CONTENTS_OFFSET];
			System.arraycopy(bytes, offset + SO_CONTENTS_OFFSET, soContentsBytes, 0, length - SO_CONTENTS_OFFSET);

			logger.trace("Attempt to parse subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
			final ReportedRouteSubobject subObj = parseSpecificSubobject(type, soContentsBytes);
			logger.trace("Subobject was parsed. {}", subObj);

			subobjsList.add(subObj);

			offset += length;
		}

		return subobjsList;
	}

	public static byte[] put(final List<ReportedRouteSubobject> objsToSerialize) {
		final List<byte[]> bytesList = new ArrayList<byte[]>(objsToSerialize.size());

		int length = 0;
		for (final ReportedRouteSubobject obj : objsToSerialize) {
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

	public static byte[] put(final ReportedRouteSubobject objToSerialize) {
		int typeIndicator = 0;

		final byte[] soContentsBytes;

		if (objToSerialize instanceof RROIPAddressSubobject<?>
				&& ((RROIPAddressSubobject<?>) objToSerialize).getPrefix() instanceof IPv4Prefix) {
			typeIndicator = PCEPSubobjectType.IPv4_PREFIX.getIndicator();
			soContentsBytes = RROIPv4AddressSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof RROIPAddressSubobject<?>
				&& ((RROIPAddressSubobject<?>) objToSerialize).getPrefix() instanceof IPv6Prefix) {
			typeIndicator = PCEPSubobjectType.IPv6_PREFIX.getIndicator();
			soContentsBytes = RROIPv6AddressSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof RROUnnumberedInterfaceSubobject) {
			typeIndicator = PCEPSubobjectType.UNNUMBERED_INTERFACE_ID.getIndicator();
			soContentsBytes = RROUnnumberedInterfaceSubobjectParser.put(objToSerialize);
		} else if (objToSerialize instanceof RROLabelSubobject) {
			typeIndicator = PCEPSubobjectType.LABEL.getIndicator();
			soContentsBytes = RROLabelSubobjectParser.put((RROLabelSubobject) objToSerialize);
		} else if (objToSerialize instanceof RROPathKeyWith32PCEIDSubobject) {
			typeIndicator = PCEPSubobjectType.PK_32.getIndicator();
			soContentsBytes = RROPathKeyWith32PCEIDSubobjectParser.put((RROPathKeyWith32PCEIDSubobject) objToSerialize);
		} else if (objToSerialize instanceof RROPathKeyWith128PCEIDSubobject) {
			typeIndicator = PCEPSubobjectType.PK_128.getIndicator();
			soContentsBytes = RROPathKeyWith128PCEIDSubobjectParser.put((RROPathKeyWith128PCEIDSubobject) objToSerialize);
		} else if (objToSerialize instanceof RROAttributesSubobject) {
			typeIndicator = PCEPSubobjectType.ATTRIBUTES.getIndicator();
			soContentsBytes = RROAttributesSubobjectParser.put((RROAttributesSubobject) objToSerialize);
		} else
			throw new IllegalArgumentException("Unknown instance of PCEPSubobject. Passed: " + objToSerialize.getClass() + ".");

		final byte[] bytes = new byte[SO_CONTENTS_OFFSET + soContentsBytes.length];

		bytes[TYPE_F_OFFSET] = ByteArray.cutBytes(ByteArray.intToBytes(typeIndicator), (Integer.SIZE / 8) - TYPE_F_LENGTH)[0];
		bytes[LENGTH_F_OFFSET] = ByteArray.cutBytes(ByteArray.intToBytes(soContentsBytes.length + SO_CONTENTS_OFFSET), (Integer.SIZE / 8)
				- LENGTH_F_LENGTH)[0];

		System.arraycopy(soContentsBytes, 0, bytes, SO_CONTENTS_OFFSET, soContentsBytes.length);

		return bytes;
	}

	private static ReportedRouteSubobject parseSpecificSubobject(final PCEPSubobjectType type, final byte[] soContentsBytes)
			throws PCEPDeserializerException {

		switch (type) {
		case IPv4_PREFIX:
			return RROIPv4AddressSubobjectParser.parse(soContentsBytes);
		case IPv6_PREFIX:
			return RROIPv6AddressSubobjectParser.parse(soContentsBytes);
		case UNNUMBERED_INTERFACE_ID:
			return RROUnnumberedInterfaceSubobjectParser.parse(soContentsBytes);
		case LABEL:
			return RROLabelSubobjectParser.parse(soContentsBytes);
		case PK_32:
			return RROPathKeyWith32PCEIDSubobjectParser.parse(soContentsBytes);
		case PK_128:
			return RROPathKeyWith128PCEIDSubobjectParser.parse(soContentsBytes);
		case ATTRIBUTES:
			return RROAttributesSubobjectParser.parse(soContentsBytes);
		default:
			throw new PCEPDeserializerException("Unknown Subobject type. Passed: " + type + ".");
		}
	}
}
