/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.c.a.list.AsSequenceBuilder;

/**
 * 
 * Representation of one AS Path Segment. It is, in fact, a TLV, but the length field is representing the count of AS
 * Numbers in the collection (in its value). If the segment is of type AS_SEQUENCE, the collection is a List, if AS_SET,
 * the collection is a Set.
 * 
 */
public final class AsPathSegmentParser {

	public static final int TYPE_LENGTH = 1;

	public static final int LENGTH_SIZE = 1;

	public static final int AS_NUMBER_LENGTH = 4;

	/**
	 * Possible types of AS Path segments.
	 */
	public enum SegmentType {
		AS_SEQUENCE, AS_SET
	}

	private AsPathSegmentParser() {

	}

	static SegmentType parseType(final int type) {
		switch (type) {
		case 1:
			return SegmentType.AS_SET;
		case 2:
			return SegmentType.AS_SEQUENCE;
		default:
			return null;
		}
	}

	static List<AsSequence> parseAsSequence(final int count, final byte[] bytes) {
		final List<AsSequence> coll = new ArrayList<>();
		int byteOffset = 0;
		for (int i = 0; i < count; i++) {
			coll.add(new AsSequenceBuilder().setAs(
					new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(bytes, byteOffset, AS_NUMBER_LENGTH)))).build());
			byteOffset += AS_NUMBER_LENGTH;
		}
		return coll;
	}

	static List<AsNumber> parseAsSet(final int count, final byte[] bytes) {
		final List<AsNumber> coll = new ArrayList<>();
		int byteOffset = 0;
		for (int i = 0; i < count; i++) {
			coll.add(new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(bytes, byteOffset, AS_NUMBER_LENGTH))));
			byteOffset += AS_NUMBER_LENGTH;
		}
		return coll;
	}
}
