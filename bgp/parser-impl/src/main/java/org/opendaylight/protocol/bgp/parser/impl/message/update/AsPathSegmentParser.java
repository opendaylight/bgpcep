/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType.AS_SEQUENCE;
import static org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathSegmentParser.SegmentType.AS_SET;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;

/**
 * Representation of one AS Path Segment. It is, in fact, a TLV, but the length field is representing the count of AS
 * Numbers in the collection (in its value). If the segment is of type AS_SEQUENCE, the collection is a List, if AS_SET,
 * the collection is a Set.
 */
public final class AsPathSegmentParser {

    public static final int AS_NUMBER_LENGTH = 4;

    /**
     * Possible types of AS Path segments.
     */
    public enum SegmentType {
        AS_SEQUENCE, AS_SET
    }

    private AsPathSegmentParser() {
        throw new UnsupportedOperationException();
    }

    static int serializeType(final SegmentType type) {
        switch (type) {
        case AS_SET:
            return 1;
        case AS_SEQUENCE:
            return 2;
        default:
            return 0;
        }
    }

    static SegmentType parseType(final int type) {
        switch (type) {
        case 1:
            return AS_SET;
        case 2:
            return AS_SEQUENCE;
        default:
            return null;
        }
    }

    static List<AsNumber> parseAsSegment(final ReferenceCache refCache, final int count, final ByteBuf buffer) {
        final List<AsNumber> coll = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            coll.add(refCache.getSharedReference(new AsNumber(buffer.readUnsignedInt())));
        }
        return (coll.isEmpty()) ? Collections.<AsNumber>emptyList() : coll;
    }

    static void serializeAsList(final List<AsNumber> asList, final SegmentType type, final ByteBuf byteAggregator) {
        if (asList == null) {
            return;
        }
        byteAggregator.writeByte(serializeType(type));
        byteAggregator.writeByte(asList.size());
        for (final AsNumber asNumber : asList) {
            byteAggregator.writeInt( asNumber.getValue().intValue());
        }
    }
}
