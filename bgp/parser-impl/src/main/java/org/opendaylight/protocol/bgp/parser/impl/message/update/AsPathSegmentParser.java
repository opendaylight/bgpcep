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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASet;

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

    static List<AsSequence> parseAsSequence(final ReferenceCache refCache, final int count, final ByteBuf buffer) {
        final List<AsSequence> coll = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coll.add(
                    refCache.getSharedReference(new AsSequenceBuilder().setAs(refCache.getSharedReference(new AsNumber(buffer.readUnsignedInt()))).build()));
        }
        return (coll.isEmpty()) ? null : coll;
    }

    static List<AsNumber> parseAsSet(final ReferenceCache refCache, final int count, final ByteBuf buffer) {
        final List<AsNumber> coll = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coll.add(refCache.getSharedReference(
                    new AsNumber(buffer.readUnsignedInt())));
        }
        return (coll.isEmpty()) ? null : coll;
    }

    static void serializeAsSet(final ASetCase aSetCase, final ByteBuf byteAggregator) {
        final ASet aset = aSetCase.getASet();
        if (aset == null || aset.getAsSet() == null) {
            return;
        }
        byteAggregator.writeByte(serializeType(AS_SET));
        byteAggregator.writeByte(aset.getAsSet().size());
        for (final AsNumber asNumber : aset.getAsSet()) {
            byteAggregator.writeInt(asNumber.getValue().intValue());
        }
    }

    static void serializeAsSequence(final AListCase aListCase, final ByteBuf byteAggregator) {
        final AList alist = aListCase.getAList();
        if (alist == null || alist.getAsSequence() == null) {
            return;
        }
        byteAggregator.writeByte(serializeType(AS_SEQUENCE));
        byteAggregator.writeByte(alist.getAsSequence().size());
        for (final AsSequence value : alist.getAsSequence()) {
            byteAggregator.writeInt(value.getAs().getValue().intValue());
        }
    }
}
