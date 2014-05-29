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
import java.util.List;

import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;

/**
 * Representation of one AS Path Segment. It is, in fact, a TLV, but the length field is representing the count of AS
 * Numbers in the collection (in its value). If the segment is of type AS_SEQUENCE, the collection is a List, if AS_SET,
 * the collection is a Set.
 */
public final class AsPathSegmentParser {

    public static final int AS_NUMBER_LENGTH = 4;

    public static final byte AS_SET_TYPE = 1;

    public static final byte AS_SEQUENCE_TYPE = 2;


    private AsPathSegmentParser() {

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

    static List<AsSequence> parseAsSequence(final ReferenceCache refCache, final int count, final ByteBuf buffer) {
        final List<AsSequence> coll = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coll.add(
                refCache.getSharedReference(
                    new AsSequenceBuilder().setAs(
                        refCache.getSharedReference(
                            new AsNumber(buffer.readUnsignedInt()))
                    ).build()
                )
            );
        }
        return coll;
    }

    static List<AsNumber> parseAsSet(final ReferenceCache refCache, final int count, final ByteBuf buffer) {
        final List<AsNumber> coll = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coll.add(refCache.getSharedReference(
                    new AsNumber(buffer.readUnsignedInt())));
        }
        return coll;
    }

    static void serializeAsSet(ASetCase aSetCase, ByteBuf byteAggregator) {
        if (aSetCase.getASet() == null || aSetCase.getASet().getAsSet() == null) {
            return;
        }
        byteAggregator.writeByte(AS_SET_TYPE);
        byteAggregator.writeByte((byte) aSetCase.getASet().getAsSet().size());
        for (AsNumber asNumber : aSetCase.getASet().getAsSet()) {
            ShortAsNumber shortAsNumber = new ShortAsNumber(asNumber);
            byteAggregator.writeInt(shortAsNumber.getValue().intValue());
        }
    }

    static void serializeAsSequence(AListCase aListCase, ByteBuf byteAggregator) {
        if (aListCase.getAList() == null || aListCase.getAList().getAsSequence() == null) {
            return;
        }
        byteAggregator.writeByte(AS_SEQUENCE_TYPE);
        byteAggregator.writeByte((byte) aListCase.getAList().getAsSequence().size());
        for (AsSequence value : aListCase.getAList().getAsSequence()) {
            ShortAsNumber shortAsNumber = new ShortAsNumber(value.getAs());
            byteAggregator.writeInt(shortAsNumber.getValue().intValue());
        }
    }

    /**
     * Possible types of AS Path segments.
     */
    public enum SegmentType {
        AS_SEQUENCE, AS_SET
    }
}
