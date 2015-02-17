/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.AdjacencyFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;

public class SrLinkAttributesParser {

    public static final int LENGTH_SIZE = 1;
    private static final int ISO_SYSTEM_ID_SIZE = 6;

    /* Adj-SID flags */
    private static int ADDRESS_FAMILY_FLAG = 0;
    private static int BACKUP_FLAG = 1;
    private static int VALUE_FLAG = 2;
    private static int LOCAL_FLAG = 3;
    private static int SET_FLAG = 4;
    private static final int FLAGS_SIZE = 1;

    public static SrAdjId parseAdjacencySegmentIdentifier(final ByteBuf buffer) {
        final SrAdjIdBuilder srAdjIdBuilder = new SrAdjIdBuilder();
        if (!buffer.isReadable()) {
            return srAdjIdBuilder.build();
        }
        final BitSet flags = BitSet.valueOf(ByteArray.readBytes(buffer, FLAGS_SIZE));
        srAdjIdBuilder.setFlags(new AdjacencyFlags(flags.get(ADDRESS_FAMILY_FLAG), flags.get(BACKUP_FLAG), flags.get(LOCAL_FLAG), flags.get(SET_FLAG), flags.get(VALUE_FLAG)));
        srAdjIdBuilder.setWeight(new Weight(Short.valueOf(buffer.readByte())));
        srAdjIdBuilder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return srAdjIdBuilder.build();
    }

    public static ByteBuf serializeAdjacencySegmentIdentifier(final SrAdjId srAdjId) {
        final ByteBuf value = Unpooled.buffer();
        final AdjacencyFlags srAdjIdFlags = srAdjId.getFlags();
        final BitSet flags = new BitSet(FLAGS_SIZE);
        if (srAdjIdFlags.isAddressFamily() != null) {
            flags.set(ADDRESS_FAMILY_FLAG, srAdjIdFlags.isAddressFamily());
        }
        if (srAdjIdFlags.isBackup() != null) {
            flags.set(BACKUP_FLAG, srAdjIdFlags.isBackup());
        }
        if (srAdjIdFlags.isValue() != null) {
            flags.set(VALUE_FLAG, srAdjIdFlags.isValue());
        }
        if (srAdjIdFlags.isLocal() != null) {
            flags.set(LOCAL_FLAG, srAdjIdFlags.isLocal());
        }
        if (srAdjIdFlags.isSetFlag() != null) {
            flags.set(SET_FLAG, srAdjIdFlags.isSetFlag());
        }
        value.writeBytes(flags.toByteArray());
        value.writeByte(srAdjId.getWeight().getValue());
        value.writeBytes(srAdjId.getSid().getValue());
        return value;
    }

    public static SrLanAdjId parseLanAdjacencySegmentIdentifier(final ByteBuf buffer) {
        final SrLanAdjIdBuilder srLanAdjIdBuilder = new SrLanAdjIdBuilder();
        if (!buffer.isReadable()) {
            return srLanAdjIdBuilder.build();
        }
        final BitSet flags = BitSet.valueOf(ByteArray.readBytes(buffer, FLAGS_SIZE));
        srLanAdjIdBuilder.setFlags(new AdjacencyFlags(flags.get(ADDRESS_FAMILY_FLAG), flags.get(BACKUP_FLAG), flags.get(LOCAL_FLAG), flags.get(SET_FLAG), flags.get(VALUE_FLAG)));
        srLanAdjIdBuilder.setWeight(new Weight(Short.valueOf(buffer.readByte())));
        srLanAdjIdBuilder.setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)));
        srLanAdjIdBuilder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return srLanAdjIdBuilder.build();
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjId srLanAdjId) {
        final ByteBuf value = Unpooled.buffer();
        final AdjacencyFlags srAdjIdFlags = srLanAdjId.getFlags();
        final BitSet flags = new BitSet(FLAGS_SIZE);
        if (srAdjIdFlags.isAddressFamily() != null) {
            flags.set(ADDRESS_FAMILY_FLAG, srAdjIdFlags.isAddressFamily());
        }
        if (srAdjIdFlags.isBackup() != null) {
            flags.set(BACKUP_FLAG, srAdjIdFlags.isBackup());
        }
        if (srAdjIdFlags.isValue() != null) {
            flags.set(VALUE_FLAG, srAdjIdFlags.isValue());
        }
        if (srAdjIdFlags.isLocal() != null) {
            flags.set(LOCAL_FLAG, srAdjIdFlags.isLocal());
        }
        if (srAdjIdFlags.isSetFlag() != null) {
            flags.set(SET_FLAG, srAdjIdFlags.isSetFlag());
        }
        value.writeBytes(flags.toByteArray());
        value.writeByte(srLanAdjId.getWeight().getValue());
        value.writeBytes(srLanAdjId.getIsoSystemId().getValue());
        value.writeBytes(srLanAdjId.getSid().getValue());
        return value;
    }
}
