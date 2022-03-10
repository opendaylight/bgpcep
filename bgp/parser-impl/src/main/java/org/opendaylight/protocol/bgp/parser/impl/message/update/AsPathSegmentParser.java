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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

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

    static ImmutableList<AsNumber> parseAsSegment(final ReferenceCache refCache, final int count,
            final ByteBuf buffer) {
        if (count == 0) {
            return ImmutableList.of();
        }

        final Builder<AsNumber> coll = ImmutableList.builderWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            coll.add(refCache.getSharedReference(new AsNumber(ByteBufUtils.readUint32(buffer))));
        }
        return coll.build();
    }

    static void serializeAsList(final Collection<AsNumber> asList, final SegmentType type,
            final ByteBuf byteAggregator) {
        if (asList == null) {
            return;
        }
        byteAggregator.writeByte(serializeType(type));
        byteAggregator.writeByte(asList.size());
        for (final AsNumber asNumber : asList) {
            byteAggregator.writeInt(asNumber.getValue().intValue());
        }
    }
}
