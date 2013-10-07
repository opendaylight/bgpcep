/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.concepts.LSPSymbolicName;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecIPv6TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.tlv.IPv4LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.IPv6LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.LSPCleanupTlv;
import org.opendaylight.protocol.pcep.tlv.LSPStateDBVersionTlv;
import org.opendaylight.protocol.pcep.tlv.LSPSymbolicNameTlv;
import org.opendaylight.protocol.pcep.tlv.LSPUpdateErrorTlv;
import org.opendaylight.protocol.pcep.tlv.NoPathVectorTlv;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;
import org.opendaylight.protocol.pcep.tlv.OFListTlv;
import org.opendaylight.protocol.pcep.tlv.OrderTlv;
import org.opendaylight.protocol.pcep.tlv.OverloadedDurationTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.pcep.tlv.RSVPErrorSpecTlv;
import org.opendaylight.protocol.pcep.tlv.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

import com.google.common.collect.Lists;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.PCEPTlv PCEPTlv} and its subclasses
 */
public final class PCEPTlvParser implements TlvParser {

    private static final Logger logger = LoggerFactory.getLogger(PCEPTlvParser.class);

    /**
     * Type indicator for {@link org.opendaylight.protocol.pcep.PCEPTlv PCEPTlv}
     */
    private enum PCEPTlvType {
	NO_PATH_VECTOR(1),
	OVERLOADED_DURATION(2),
	REQ_MISSING(3),
	OF_LIST_TLV(4),
	ORDER_TLV(5),
	PCE_STATEFUL_CAPABILITY(16),
	LSP_SYMBOLIC_NAME(17),
	LSP_IDENTIFIER_IPV4(18),
	LSP_IDENTIFIER_IPV6(19),
	LSP_UPDATE_ERROR(20),
	RSVP_ERROR_SPEC_IPV4(21),
	RSVP_ERROR_SPEC_IPV6(22),
	LSP_STATE_DB_VERSION(23),
	// TODO: use IANA defined number - for now has been used first unused
	// number
	NODE_IDENTIFIER(24),
	LSP_CLEANUP_TLV(26);

	private final int indicator;

	PCEPTlvType(final int indicator) {
	    this.indicator = indicator;
	}

	public int getIndicator() {
	    return this.indicator;
	}

	public static PCEPTlvType getFromInt(final int type) throws PCEPDeserializerException {

	    for (final PCEPTlvType type_e : PCEPTlvType.values()) {
		if (type_e.getIndicator() == type)
		    return type_e;
	    }

	    throw new PCEPDeserializerException("Unknown TLV type: " + type);
	}
	}

    /*
     * Fields lengths in Bytes
     */
    public static final int TYPE_F_LENGTH = 2;
    public static final int LENGTH_F_LENGTH = 2;
    public static final int HEADER_LENGTH = LENGTH_F_LENGTH + TYPE_F_LENGTH;

    /*
     * Fields offsets in Bytes
     */
    public static final int TYPE_F_OFFSET = 0;
    public static final int LENGTH_F_OFFSET = TYPE_F_OFFSET + TYPE_F_LENGTH;
    public static final int VALUE_F_OFFSET = LENGTH_F_OFFSET + LENGTH_F_LENGTH;

    /*
     * padding of value field in bytes
     */
    public static final int PADDED_TO = 4;

    /*
     * constants for specific one-value tlvs
     */
    private static final int DBV_F_LENGTH = 8;
    private static final int UPDATE_ERR_CODE_LENGTH = 4;
    private static final int REQ_ID_LENGTH = 4;
    private static final int ORDR_DEL_LENGTH = 4;
    private static final int ORDR_SETUP_LENGTH = 4;
    private static final int P2MP_CAPABLITY_LENGTH = 2;

    public List<Tlv> parseTlv(final byte[] bytes) throws PCEPDeserializerException {
	if (bytes == null)
	    throw new IllegalArgumentException("Byte array is mandatory.");

	final List<Tlv> tlvList = Lists.newArrayList();
	PCEPTlvType type;
	int length;
	int offset = 0;

	while (offset + HEADER_LENGTH < bytes.length) {

	    length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, LENGTH_F_LENGTH));

	    type = PCEPTlvType.getFromInt(ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + TYPE_F_OFFSET, TYPE_F_LENGTH)));

	    if (HEADER_LENGTH + length > bytes.length - offset)
		throw new PCEPDeserializerException("Wrong length specified. Passed: " + (HEADER_LENGTH + length) + "; Expected: <= " + (bytes.length - offset)
			+ ".");

	    final byte[] tlvBytes = ByteArray.subByte(bytes, offset + VALUE_F_OFFSET, length);

	    logger.trace("Attempt to parse tlv from bytes: {}", ByteArray.bytesToHexString(tlvBytes));
	    final Tlv tlv = parseSpecificTLV(type, tlvBytes);
	    logger.trace("Tlv was parsed. {}", tlv);

	    tlvList.add(tlv);

	    offset += HEADER_LENGTH + length + Util.getPadding(HEADER_LENGTH + length, PADDED_TO);
	}

	return tlvList;
    }

    public static byte[] put(final List<PCEPTlv> objsToSerialize) {
	final List<byte[]> bytesList = new ArrayList<byte[]>(objsToSerialize.size());

	int length = 0;
	for (final PCEPTlv obj : objsToSerialize) {
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

    public static byte[] put(final PCEPTlv objToSerialize) {
	int typeIndicator = 0;

	byte[] valueBytes;

	if (objToSerialize instanceof PCEStatefulCapabilityTlv) {
	    typeIndicator = PCEPTlvType.PCE_STATEFUL_CAPABILITY.getIndicator();
	    valueBytes = PCEStatefulCapabilityTlvParser.serializeValueField((PCEStatefulCapabilityTlv) objToSerialize);
	} else if (objToSerialize instanceof LSPStateDBVersionTlv) {
	    typeIndicator = PCEPTlvType.LSP_STATE_DB_VERSION.getIndicator();
	    valueBytes = ByteArray.longToBytes(((LSPStateDBVersionTlv) objToSerialize).getDbVersion());
	} else if (objToSerialize instanceof NoPathVectorTlv) {
	    typeIndicator = PCEPTlvType.NO_PATH_VECTOR.getIndicator();
	    valueBytes = NoPathVectorTlvParser.put((NoPathVectorTlv) objToSerialize);
	} else if (objToSerialize instanceof OverloadedDurationTlv) {
	    typeIndicator = PCEPTlvType.OVERLOADED_DURATION.getIndicator();
	    valueBytes = ByteArray.intToBytes(((OverloadedDurationTlv) objToSerialize).getValue());
	} else if (objToSerialize instanceof LSPSymbolicNameTlv) {
	    typeIndicator = PCEPTlvType.LSP_SYMBOLIC_NAME.getIndicator();
	    valueBytes = ((LSPSymbolicNameTlv) objToSerialize).getSymbolicName().getSymbolicName();
	} else if (objToSerialize instanceof LSPUpdateErrorTlv) {
	    typeIndicator = PCEPTlvType.LSP_UPDATE_ERROR.getIndicator();
	    valueBytes = ((LSPUpdateErrorTlv) objToSerialize).getErrorCode();

	    assert valueBytes.length == UPDATE_ERR_CODE_LENGTH : "Update error code si too large.";

	} else if (objToSerialize instanceof IPv4LSPIdentifiersTlv) {
	    typeIndicator = PCEPTlvType.LSP_IDENTIFIER_IPV4.getIndicator();
	    valueBytes = LSPIdentifierIPv4TlvParser.put((IPv4LSPIdentifiersTlv) objToSerialize);
	} else if (objToSerialize instanceof IPv6LSPIdentifiersTlv) {
	    typeIndicator = PCEPTlvType.LSP_IDENTIFIER_IPV6.getIndicator();
	    valueBytes = LSPIdentifierIPv6TlvParser.put((IPv6LSPIdentifiersTlv) objToSerialize);
	} else if (objToSerialize instanceof RSVPErrorSpecTlv<?> && ((RSVPErrorSpecTlv<?>) objToSerialize).getErrorNodeAddress() instanceof IPv4Address) {
	    typeIndicator = PCEPTlvType.RSVP_ERROR_SPEC_IPV4.getIndicator();
	    valueBytes = RSVPErrorSpecIPv4TlvParser.put((RSVPErrorSpecTlv<?>) objToSerialize);
	} else if (objToSerialize instanceof RSVPErrorSpecTlv<?> && ((RSVPErrorSpecTlv<?>) objToSerialize).getErrorNodeAddress() instanceof IPv6Address) {
	    typeIndicator = PCEPTlvType.RSVP_ERROR_SPEC_IPV6.getIndicator();
	    valueBytes = RSVPErrorSpecIPv6TlvParser.put((RSVPErrorSpecTlv<?>) objToSerialize);
	} else if (objToSerialize instanceof ReqMissingTlv) {
	    typeIndicator = PCEPTlvType.REQ_MISSING.getIndicator();
	    valueBytes = new byte[REQ_ID_LENGTH];
	    System.arraycopy(ByteArray.longToBytes(((ReqMissingTlv) objToSerialize).getRequestID()), Long.SIZE / Byte.SIZE - REQ_ID_LENGTH, valueBytes, 0,
		    REQ_ID_LENGTH);
	} else if (objToSerialize instanceof NodeIdentifierTlv) {
	    typeIndicator = PCEPTlvType.NODE_IDENTIFIER.getIndicator();
	    valueBytes = ((NodeIdentifierTlv) objToSerialize).getValue();
	} else if (objToSerialize instanceof OrderTlv) {
	    typeIndicator = PCEPTlvType.ORDER_TLV.getIndicator();
	    valueBytes = new byte[ORDR_DEL_LENGTH + ORDR_SETUP_LENGTH];
	    ByteArray.copyWhole(ByteArray.intToBytes((int) ((OrderTlv) objToSerialize).getDeleteOrder()), valueBytes, 0);
	    ByteArray.copyWhole(ByteArray.intToBytes((int) ((OrderTlv) objToSerialize).getSetupOrder()), valueBytes, ORDR_DEL_LENGTH);
	} else if (objToSerialize instanceof OFListTlv) {
	    typeIndicator = PCEPTlvType.OF_LIST_TLV.getIndicator();
	    valueBytes = OFListTlvParser.put((OFListTlv) objToSerialize);
	} else if (objToSerialize instanceof LSPCleanupTlv) {
	    typeIndicator = PCEPTlvType.LSP_CLEANUP_TLV.getIndicator();
	    valueBytes = ByteArray.intToBytes(((LSPCleanupTlv) objToSerialize).getTimeout());
	} else
	    throw new IllegalArgumentException("Unknown instance of PCEPTlv. Passed: " + objToSerialize + ".");

	final byte[] typeBytes = ByteArray.cutBytes(ByteArray.intToBytes(typeIndicator), (Integer.SIZE / 8) - TYPE_F_LENGTH);
	final byte[] lengthBytes = ByteArray.cutBytes(ByteArray.intToBytes(valueBytes.length), (Integer.SIZE / 8) - LENGTH_F_LENGTH);
	final byte[] bytes = new byte[HEADER_LENGTH + valueBytes.length + Util.getPadding(HEADER_LENGTH + valueBytes.length, PADDED_TO)];

	System.arraycopy(typeBytes, 0, bytes, TYPE_F_OFFSET, TYPE_F_LENGTH);
	System.arraycopy(lengthBytes, 0, bytes, LENGTH_F_OFFSET, LENGTH_F_LENGTH);
	System.arraycopy(valueBytes, 0, bytes, VALUE_F_OFFSET, valueBytes.length);

	return bytes;
    }

    private static PCEPTlv parseSpecificTLV(final PCEPTlvType type, final byte[] valueBytes) throws PCEPDeserializerException {
	switch (type) {
	    case PCE_STATEFUL_CAPABILITY:
		return PCEStatefulCapabilityTlvParser.deserializeValueField(valueBytes);
	    case LSP_STATE_DB_VERSION:
		return new LSPStateDBVersionTlv(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, 0, DBV_F_LENGTH)));
	    case NO_PATH_VECTOR:
		return NoPathVectorTlvParser.parse(valueBytes);
	    case OVERLOADED_DURATION:
		return new OverloadedDurationTlv(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, 0, OVERLOADED_DURATION_LENGTH)));
	    case LSP_SYMBOLIC_NAME:
		return new LSPSymbolicNameTlv(new LSPSymbolicName(valueBytes));
	    case LSP_UPDATE_ERROR:
		return new LSPUpdateErrorTlv(valueBytes);
	    case LSP_IDENTIFIER_IPV4:
		return LSPIdentifierIPv4TlvParser.parse(valueBytes);
	    case LSP_IDENTIFIER_IPV6:
		return LSPIdentifierIPv6TlvParser.parse(valueBytes);
	    case RSVP_ERROR_SPEC_IPV4:
		return RSVPErrorSpecIPv4TlvParser.parse(valueBytes);
	    case RSVP_ERROR_SPEC_IPV6:
		return RSVPErrorSpecIPv6TlvParser.parse(valueBytes);
	    case REQ_MISSING:
		return new ReqMissingTlv(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, 0, REQ_ID_LENGTH)));
	    case NODE_IDENTIFIER:
		return new NodeIdentifierTlv(valueBytes);
	    case ORDER_TLV:
		return new OrderTlv(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, 0, ORDR_DEL_LENGTH)), ByteArray.bytesToLong(ByteArray.subByte(
			valueBytes, ORDR_DEL_LENGTH, ORDR_SETUP_LENGTH)));
	    case OF_LIST_TLV:
		return OFListTlvParser.parse(valueBytes);
	    case LSP_CLEANUP_TLV:
		return new LSPCleanupTlv(ByteArray.bytesToInt(valueBytes));
	    default:
		throw new PCEPDeserializerException("Unknown TLV type. Passed: " + type + ";");
	}
    }
}
