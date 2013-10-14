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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROExplicitExclusionRouteSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIPv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIPv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKeyWith128PCEIDSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKeyWith32PCEIDSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROProtectionSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.subobject.EROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.EROExplicitExclusionRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.EROLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROPathKeyWith128PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.EROPathKeyWith32PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.EROProtectionSubobject;
import org.opendaylight.protocol.pcep.subobject.EROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.PCEPSubobject PCEPSubobject}
 */
public class PCEPEROSubobjectParser {

    private static final Logger logger = LoggerFactory.getLogger(PCEPEROSubobjectParser.class);

    /**
     * Type identifier for {@link org.opendaylight.protocol.pcep.PCEPSubobject
     * PCEPSubobject}
     */
    public enum PCEPSubobjectType {
		IPv4_PREFIX(1), IPv6_PREFIX(2), LABEL(3), UNNUMBERED_INTERFACE_ID(4), AS_NUMBER(32), EXRS(33), PROTECTION(37), PK_32(
				64), PK_128(65);

		private final int indicator;

		PCEPSubobjectType(int indicator) {
			this.indicator = indicator;
		}

		public int getIndicator() {
			return this.indicator;
		}

		public static PCEPSubobjectType getFromInt(int type) throws PCEPDeserializerException {

			for (final PCEPSubobjectType type_e : PCEPSubobjectType.values()) {
				if (type_e.getIndicator() == type)
					return type_e;
			}

			throw new PCEPDeserializerException("Unknown Subobject type. Passed: " + type + "; Known: "
					+ PCEPSubobjectType.values() + ".");
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

    public static List<ExplicitRouteSubobject> parse(byte[] bytes) throws PCEPDeserializerException {
	if (bytes == null)
	    throw new IllegalArgumentException("Byte array is mandatory.");

	final List<ExplicitRouteSubobject> subobjsList = new ArrayList<ExplicitRouteSubobject>();
	boolean loose_flag;
	PCEPSubobjectType type;
	byte[] soContentsBytes;
	int length;
	int offset = 0;

	while (offset < bytes.length) {

	    loose_flag = ((bytes[offset + TYPE_FLAG_F_OFFSET] & (1 << 7)) != 0) ? true : false;
	    length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, LENGTH_F_LENGTH));

	    type = PCEPSubobjectType.getFromInt((bytes[offset + TYPE_FLAG_F_OFFSET] & 0xff) & ~(1 << 7));

	    if (length > bytes.length - offset)
		throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= " + (bytes.length - offset));

	    soContentsBytes = new byte[length - SO_CONTENTS_OFFSET];
	    System.arraycopy(bytes, offset + SO_CONTENTS_OFFSET, soContentsBytes, 0, length - SO_CONTENTS_OFFSET);

	    logger.debug("Attempt to parse subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
	    final ExplicitRouteSubobject subObj = parseSpecificSubobject(type, soContentsBytes, loose_flag);
	    logger.debug("Subobject was parsed. {}", subObj);

	    subobjsList.add(subObj);

	    offset += length;
	}

	return subobjsList;
    }

    public static byte[] put(List<ExplicitRouteSubobject> objsToSerialize) {
	final List<byte[]> bytesList = new ArrayList<byte[]>(objsToSerialize.size());

	int length = 0;
	for (final ExplicitRouteSubobject obj : objsToSerialize) {
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

    public static byte[] put(ExplicitRouteSubobject objToSerialize) {
	int typeIndicator = 0;

	final byte[] soContentsBytes;

	if (objToSerialize instanceof EROIPPrefixSubobject<?> && ((EROIPPrefixSubobject<?>) objToSerialize).getPrefix() instanceof IPv4Prefix) {
	    typeIndicator = PCEPSubobjectType.IPv4_PREFIX.getIndicator();
	    soContentsBytes = EROIPv4PrefixSubobjectParser.put(objToSerialize);
	} else if (objToSerialize instanceof EROIPPrefixSubobject<?> && ((EROIPPrefixSubobject<?>) objToSerialize).getPrefix() instanceof IPv6Prefix) {
	    typeIndicator = PCEPSubobjectType.IPv6_PREFIX.getIndicator();
	    soContentsBytes = EROIPv6PrefixSubobjectParser.put(objToSerialize);
	} else if (objToSerialize instanceof EROAsNumberSubobject) {
	    typeIndicator = PCEPSubobjectType.AS_NUMBER.getIndicator();
	    soContentsBytes = EROAsNumberSubobjectParser.put(objToSerialize);
	} else if (objToSerialize instanceof EROUnnumberedInterfaceSubobject) {
	    typeIndicator = PCEPSubobjectType.UNNUMBERED_INTERFACE_ID.getIndicator();
	    soContentsBytes = EROUnnumberedInterfaceSubobjectParser.put(objToSerialize);
	} else if (objToSerialize instanceof EROLabelSubobject) {
	    typeIndicator = PCEPSubobjectType.LABEL.getIndicator();
	    soContentsBytes = EROLabelSubobjectParser.put((EROLabelSubobject) objToSerialize);
	} else if (objToSerialize instanceof EROExplicitExclusionRouteSubobject) {
	    typeIndicator = PCEPSubobjectType.EXRS.getIndicator();
	    soContentsBytes = EROExplicitExclusionRouteSubobjectParser.put((EROExplicitExclusionRouteSubobject) objToSerialize);
	} else if (objToSerialize instanceof EROPathKeyWith32PCEIDSubobject) {
	    typeIndicator = PCEPSubobjectType.PK_32.getIndicator();
	    soContentsBytes = EROPathKeyWith32PCEIDSubobjectParser.put((EROPathKeyWith32PCEIDSubobject) objToSerialize);
	} else if (objToSerialize instanceof EROPathKeyWith128PCEIDSubobject) {
	    typeIndicator = PCEPSubobjectType.PK_128.getIndicator();
	    soContentsBytes = EROPathKeyWith128PCEIDSubobjectParser.put((EROPathKeyWith128PCEIDSubobject) objToSerialize);
	} else if (objToSerialize instanceof EROProtectionSubobject) {
	    typeIndicator = PCEPSubobjectType.PROTECTION.getIndicator();
	    soContentsBytes = EROProtectionSubobjectParser.put((EROProtectionSubobject) objToSerialize);
	} else
	    throw new IllegalArgumentException("Unknown instance of PCEPSubobject. Passed: " + objToSerialize.getClass() + ".");

	final byte[] bytes = new byte[SO_CONTENTS_OFFSET + soContentsBytes.length];

	bytes[TYPE_FLAG_F_OFFSET] = (byte) (ByteArray.cutBytes(ByteArray.intToBytes(typeIndicator), (Integer.SIZE / 8) - TYPE_FLAG_F_LENGTH)[0] | (objToSerialize
		.isLoose() ? 1 << 7 : 0));
	bytes[LENGTH_F_OFFSET] = ByteArray.cutBytes(ByteArray.intToBytes(soContentsBytes.length + SO_CONTENTS_OFFSET), (Integer.SIZE / 8) - LENGTH_F_LENGTH)[0];

	System.arraycopy(soContentsBytes, 0, bytes, SO_CONTENTS_OFFSET, soContentsBytes.length);

	return bytes;
    }

    private static ExplicitRouteSubobject parseSpecificSubobject(PCEPSubobjectType type, byte[] soContentsBytes, boolean loose_flag)
	    throws PCEPDeserializerException {

	switch (type) {
	    case IPv4_PREFIX:
		return EROIPv4PrefixSubobjectParser.parse(soContentsBytes, loose_flag);
	    case IPv6_PREFIX:
		return EROIPv6PrefixSubobjectParser.parse(soContentsBytes, loose_flag);
	    case UNNUMBERED_INTERFACE_ID:
		return EROUnnumberedInterfaceSubobjectParser.parse(soContentsBytes, loose_flag);
	    case AS_NUMBER:
		return EROAsNumberSubobjectParser.parse(soContentsBytes, loose_flag);
	    case LABEL:
		return EROLabelSubobjectParser.parse(soContentsBytes, loose_flag);
	    case EXRS:
		return EROExplicitExclusionRouteSubobjectParser.parse(soContentsBytes, loose_flag);
	    case PK_32:
		return EROPathKeyWith32PCEIDSubobjectParser.parse(soContentsBytes, loose_flag);
	    case PK_128:
		return EROPathKeyWith128PCEIDSubobjectParser.parse(soContentsBytes, loose_flag);
	    case PROTECTION:
		return EROProtectionSubobjectParser.parse(soContentsBytes, loose_flag);
	    default:
		throw new PCEPDeserializerException("Unknown Subobject type. Passed: " + type + ".");
	}
    }
}
