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
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
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
        final Short adjFlags;
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            adjFlags = buffer.readUnsignedByte();
            weight = new Weight(buffer.readUnsignedByte());
            sidValue = SidLabelIndexParser.parseSidSubTlv(buffer);
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
            public Short getFlags() {
                return adjFlags;
            }
        };
    }

    public static SrLanAdjId parseLanAdjacencySegmentIdentifier(final ByteBuf buffer) {
        if (!buffer.isReadable()) {
            return new SrLanAdjIdBuilder().build();
        }
        final SrLanAdjIdBuilder srLanAdjIdBuilder = new SrLanAdjIdBuilder();
        srLanAdjIdBuilder.setFlags(buffer.readUnsignedByte());
        srLanAdjIdBuilder.setWeight(new Weight(buffer.readUnsignedByte()));
        srLanAdjIdBuilder.setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)));
        // length determines a type of next field, which is used for parsing
        srLanAdjIdBuilder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer));
        return srLanAdjIdBuilder.build();
    }

    public static ByteBuf serializeAdjacencySegmentIdentifier(final AdjSidTlv adjSid) {
        final ByteBuf value = Unpooled.buffer();
        value.writeByte(adjSid.getFlags());
        value.writeByte(adjSid.getWeight().getValue());
        TlvUtil.writeTLV(SidLabelIndexParser.SID_TYPE, SidLabelIndexParser.serializeSidValue(adjSid.getSidLabelIndex()), value);
        return value;
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjId srLanAdjId) {
        final ByteBuf value = Unpooled.buffer();
        value.writeByte(srLanAdjId.getFlags());
        value.writeByte(srLanAdjId.getWeight().getValue());
        value.writeBytes(srLanAdjId.getIsoSystemId().getValue());
        TlvUtil.writeTLV(SidLabelIndexParser.SID_TYPE, SidLabelIndexParser.serializeSidValue(srLanAdjId.getSidLabelIndex()), value);
        return value;
    }

}
