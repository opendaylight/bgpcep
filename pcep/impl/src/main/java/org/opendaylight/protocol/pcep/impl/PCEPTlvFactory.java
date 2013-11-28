/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.tlv.LSPCleanupTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPStateDBVersionTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NodeIdentifierTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.P2MPCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecIPv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
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
import org.opendaylight.protocol.pcep.tlv.P2MPCapabilityTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.pcep.tlv.RSVPErrorSpecTlv;
import org.opendaylight.protocol.pcep.tlv.ReqMissingTlv;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.PCEPTlv PCEPTlv} and its subclasses
 */
public final class PCEPTlvFactory {

	private static final Logger logger = LoggerFactory.getLogger(PCEPTlvFactory.class);

	private static class MapOfParsers extends HashMap<Integer, PCEPTlvParser> {

		private static final long serialVersionUID = 1L;

		private final static MapOfParsers instance = new MapOfParsers();

		private MapOfParsers() {
			fillInMap();
		}

		private void fillInMap() {
			this.put(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
			this.put(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
			this.put(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
			this.put(OFListTlvParser.TYPE, new OFListTlvParser());
			this.put(OrderTlvParser.TYPE, new OrderTlvParser());
			this.put(P2MPCapabilityTlvParser.TYPE, new P2MPCapabilityTlvParser());
			this.put(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser());
			this.put(LSPSymbolicNameTlvParser.TYPE, new LSPSymbolicNameTlvParser());
			this.put(LSPIdentifierIPv4TlvParser.TYPE, new LSPIdentifierIPv4TlvParser());
			this.put(LSPIdentifierIPv6TlvParser.TYPE, new LSPIdentifierIPv6TlvParser());
			this.put(LSPUpdateErrorTlvParser.TYPE, new LSPUpdateErrorTlvParser());
			this.put(RSVPErrorSpecIPv4TlvParser.TYPE, new RSVPErrorSpecIPv4TlvParser());
			this.put(RSVPErrorSpecIPv6TlvParser.TYPE, new RSVPErrorSpecIPv6TlvParser());
			this.put(LSPStateDBVersionTlvParser.TYPE, new LSPStateDBVersionTlvParser());
			this.put(NodeIdentifierTlvParser.TYPE, new NodeIdentifierTlvParser());
			this.put(LSPCleanupTlvParser.TYPE, new LSPCleanupTlvParser());
		}

		public static MapOfParsers getInstance() {
			return instance;
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

	public static List<PCEPTlv> parse(final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		final List<PCEPTlv> tlvList = new ArrayList<PCEPTlv>();
		int type;
		int length;
		int offset = 0;

		while (offset + HEADER_LENGTH < bytes.length) {

			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, LENGTH_F_LENGTH));

			type = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + TYPE_F_OFFSET, TYPE_F_LENGTH));

			if (HEADER_LENGTH + length > bytes.length - offset)
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + (HEADER_LENGTH + length) + "; Expected: <= "
						+ (bytes.length - offset) + ".");

			final byte[] tlvBytes = ByteArray.subByte(bytes, offset + VALUE_F_OFFSET, length);

			logger.trace("Attempt to parse tlv from bytes: {}", ByteArray.bytesToHexString(tlvBytes));

			final PCEPTlvParser parser = MapOfParsers.getInstance().get(type);

			if (parser == null) {
				logger.warn("Unprocessed tlv type {} was ignored.", type);
				offset += HEADER_LENGTH + length + Util.getPadding(HEADER_LENGTH + length, PADDED_TO);
				continue;
			}

			final PCEPTlv tlv = parser.parse(tlvBytes);

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

		if (objToSerialize instanceof PCEStatefulCapabilityTlv) {
			typeIndicator = PCEStatefulCapabilityTlvParser.TYPE;
		} else if (objToSerialize instanceof LSPStateDBVersionTlv) {
			typeIndicator = LSPStateDBVersionTlvParser.TYPE;
		} else if (objToSerialize instanceof NoPathVectorTlv) {
			typeIndicator = NoPathVectorTlvParser.TYPE;
		} else if (objToSerialize instanceof OverloadedDurationTlv) {
			typeIndicator = OverloadedDurationTlvParser.TYPE;
		} else if (objToSerialize instanceof LSPSymbolicNameTlv) {
			typeIndicator = LSPSymbolicNameTlvParser.TYPE;
		} else if (objToSerialize instanceof LSPUpdateErrorTlv) {
			typeIndicator = LSPUpdateErrorTlvParser.TYPE;
		} else if (objToSerialize instanceof IPv4LSPIdentifiersTlv) {
			typeIndicator = LSPIdentifierIPv4TlvParser.TYPE;
		} else if (objToSerialize instanceof IPv6LSPIdentifiersTlv) {
			typeIndicator = LSPIdentifierIPv6TlvParser.TYPE;
		} else if (objToSerialize instanceof RSVPErrorSpecTlv<?>
				&& ((RSVPErrorSpecTlv<?>) objToSerialize).getErrorNodeAddress() instanceof IPv4Address) {
			typeIndicator = RSVPErrorSpecIPv4TlvParser.TYPE;
		} else if (objToSerialize instanceof RSVPErrorSpecTlv<?>
				&& ((RSVPErrorSpecTlv<?>) objToSerialize).getErrorNodeAddress() instanceof IPv6Address) {
			typeIndicator = RSVPErrorSpecIPv6TlvParser.TYPE;
		} else if (objToSerialize instanceof ReqMissingTlv) {
			typeIndicator = ReqMissingTlvParser.TYPE;
		} else if (objToSerialize instanceof NodeIdentifierTlv) {
			typeIndicator = NodeIdentifierTlvParser.TYPE;
		} else if (objToSerialize instanceof OrderTlv) {
			typeIndicator = OrderTlvParser.TYPE;
		} else if (objToSerialize instanceof P2MPCapabilityTlv) {
			typeIndicator = P2MPCapabilityTlvParser.TYPE;
		} else if (objToSerialize instanceof OFListTlv) {
			typeIndicator = OFListTlvParser.TYPE;
		} else if (objToSerialize instanceof LSPCleanupTlv) {
			typeIndicator = LSPCleanupTlvParser.TYPE;
		} else
			throw new IllegalArgumentException("Unknown instance of PCEPTlv. Passed: " + objToSerialize + ".");

		final PCEPTlvParser tlvParserClass = MapOfParsers.getInstance().get(typeIndicator);

		final byte[] valueBytes = tlvParserClass.put(objToSerialize);

		final byte[] typeBytes = ByteArray.cutBytes(ByteArray.intToBytes(typeIndicator), (Integer.SIZE / 8) - TYPE_F_LENGTH);
		final byte[] lengthBytes = ByteArray.cutBytes(ByteArray.intToBytes(valueBytes.length), (Integer.SIZE / 8) - LENGTH_F_LENGTH);
		final byte[] bytes = new byte[HEADER_LENGTH + valueBytes.length + Util.getPadding(HEADER_LENGTH + valueBytes.length, PADDED_TO)];

		System.arraycopy(typeBytes, 0, bytes, TYPE_F_OFFSET, TYPE_F_LENGTH);
		System.arraycopy(lengthBytes, 0, bytes, LENGTH_F_OFFSET, LENGTH_F_LENGTH);
		System.arraycopy(valueBytes, 0, bytes, VALUE_F_OFFSET, valueBytes.length);

		return bytes;
	}
}
