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
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.AdjSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SrLinkAttributesParser {

    private static final int ISO_SYSTEM_ID_SIZE = 6;
    private static final int RESERVED = 2;

    /** OSPF flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |B|V|L|S|       |
      +-+-+-+-+-+-+-+-+

       ISIS flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |F|B|V|L|S|     |
      +-+-+-+-+-+-+-+-+
     */

    private SrLinkAttributesParser() {
        throw new UnsupportedOperationException();
    }

    public static AdjSidTlv parseAdjacencySegmentIdentifier(final ByteBuf buffer) {
        final byte[] adjFlags;
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            adjFlags = new byte[] { (byte) buffer.readUnsignedByte() };
            weight = new Weight(buffer.readUnsignedByte());
            buffer.skipBytes(RESERVED);
            sidValue = SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer);
        } else {
            adjFlags = null;
            weight = null;
            sidValue = null;
        }
        return new AdjSidTlv() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return AdjSidTlv.class;
            }
            @Override
            public Weight getWeight() {
                return weight;
            }
            @Override
            public SidLabelIndex getSidLabelIndex() {
                return sidValue;
            }
            @Override
            public byte[] getFlags() {
                return adjFlags;
            }
        };
    }

    public static SrLanAdjId parseLanAdjacencySegmentIdentifier(final ByteBuf buffer) {
        if (!buffer.isReadable()) {
            return new SrLanAdjIdBuilder().build();
        }
        final SrLanAdjIdBuilder srLanAdjIdBuilder = new SrLanAdjIdBuilder();
        srLanAdjIdBuilder.setFlags(new byte[] { (byte) buffer.readUnsignedByte() });
        srLanAdjIdBuilder.setWeight(new Weight(buffer.readUnsignedByte()));
        buffer.skipBytes(RESERVED);
        srLanAdjIdBuilder.setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)));
        // length determines a type of next field, which is used for parsing
        srLanAdjIdBuilder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer));
        return srLanAdjIdBuilder.build();
    }

    public static ByteBuf serializeAdjacencySegmentIdentifier(final AdjSidTlv adjSid) {
        final ByteBuf value = Unpooled.buffer();
        value.writeBytes(adjSid.getFlags());
        value.writeByte(adjSid.getWeight().getValue());
        value.writeZero(RESERVED);
        value.writeBytes(SidLabelIndexParser.serializeSidValue(adjSid.getSidLabelIndex()));
        return value;
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjId srLanAdjId) {
        final ByteBuf value = Unpooled.buffer();
        value.writeBytes(srLanAdjId.getFlags());
        value.writeByte(srLanAdjId.getWeight().getValue());
        value.writeZero(RESERVED);
        value.writeBytes(srLanAdjId.getIsoSystemId().getValue());
        value.writeBytes(SidLabelIndexParser.serializeSidValue(srLanAdjId.getSidLabelIndex()));
        return value;
    }

}
