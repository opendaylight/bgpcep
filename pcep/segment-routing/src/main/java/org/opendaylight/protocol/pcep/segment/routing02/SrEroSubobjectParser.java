/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SrEroSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SrEroSubobject.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.Nai;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.IpAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.IpAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.UnnumberedAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.UnnumberedAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;

public class SrEroSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

    public static final int TYPE = 5;

    private static final int SID_LENGTH = 4;
    private static final int FLAGS_OFFSET = 1;
    private static final int HEADER_LENGTH = FLAGS_OFFSET + 1;
    private static final int MINIMAL_LENGTH = SID_LENGTH + HEADER_LENGTH;

    @Override
    public void serializeSubobject(Subobject subobject, final ByteBuf buffer) {
        if (subobject.getSubobjectType() == null) {
            throw new IllegalArgumentException("Unknown subobject instance. Needed SrEroSubobject.");
        }
        final SrEroSubobject srEroSubobject = (SrEroSubobject) subobject.getSubobjectType();
        final ByteBuf body = Unpooled.buffer(MINIMAL_LENGTH);
        body.writeByte(reverseBits(UnsignedBytes.checkedCast(srEroSubobject.getSidType().getIntValue())));

        final Flags flags = srEroSubobject.getFlags();
        final BitSet bits = new BitSet();
        bits.set(7, flags.isM());
        bits.set(6, flags.isC());
        bits.set(5, flags.isS());
        bits.set(4, flags.isF());
        body.writeByte(ByteArray.bitSetToBytes(bits, FLAGS_OFFSET)[0]);

        if(srEroSubobject.getSid() != null) {
            body.writeInt(srEroSubobject.getSid().intValue());
        }

        final Nai nai = srEroSubobject.getNai();
        if(nai != null) {
            switch (srEroSubobject.getSidType()) {
            case Ipv4NodeId:
                body.writeBytes(Ipv4Util.bytesForAddress(((IpNodeId)nai).getIpAddress().getIpv4Address()));
                break;
            case Ipv6NodeId:
                body.writeBytes(Ipv6Util.bytesForAddress(((IpNodeId)nai).getIpAddress().getIpv6Address()));
                break;
            case Ipv4Adjacency:
                body.writeBytes(Ipv4Util.bytesForAddress(((IpAdjacency)nai).getLocalIpAddress().getIpv4Address()));
                body.writeBytes(Ipv4Util.bytesForAddress(((IpAdjacency)nai).getRemoteIpAddress().getIpv4Address()));
                break;
            case Ipv6Adjacency:
                body.writeBytes(Ipv6Util.bytesForAddress(((IpAdjacency)nai).getLocalIpAddress().getIpv6Address()));
                body.writeBytes(Ipv6Util.bytesForAddress(((IpAdjacency)nai).getRemoteIpAddress().getIpv6Address()));
                break;
            case Unnumbered:
                final UnnumberedAdjacency unnumbered = (UnnumberedAdjacency)nai;
                body.writeInt(unnumbered.getLocalNodeId().intValue());
                body.writeInt(unnumbered.getLocalInterfaceId().intValue());
                body.writeInt(unnumbered.getRemoteNodeId().intValue());
                body.writeInt(unnumbered.getRemoteInterfaceId().intValue());
                break;
            }
        }
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

    @Override
    public Subobject parseSubobject(ByteBuf buffer, boolean loose) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() <= MINIMAL_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes()
                    + ";");
        }
        final SrEroTypeBuilder srEroSubobjectBuilder = new SrEroTypeBuilder();
        final int sidTypeByte = reverseBits(buffer.readByte());
        final SidType sidType = SidType.forValue(sidTypeByte);
        srEroSubobjectBuilder.setSidType(sidType);

        BitSet bitSet = ByteArray.bytesToBitSet(new byte[] { buffer.readByte() });
        final boolean f = bitSet.get(4);
        final boolean s = bitSet.get(5);
        final boolean c = bitSet.get(6);
        final boolean m = bitSet.get(7);
        final Flags flags = new Flags(c, f, m, s);
        srEroSubobjectBuilder.setFlags(flags);

        final long sid = buffer.readUnsignedInt();
        srEroSubobjectBuilder.setSid(sid);
        if(sidType != null) {
            switch (sidType) {
            case Ipv4NodeId:
                srEroSubobjectBuilder.setNai(new IpNodeIdBuilder()
                        .setIpAddress(new IpAddress(new Ipv4Address(Ipv4Util.addressForByteBuf(buffer)))).build());
                break;
            case Ipv6NodeId:
                srEroSubobjectBuilder.setNai(new IpNodeIdBuilder().setIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer))).build());
                break;
            case Ipv4Adjacency:
                srEroSubobjectBuilder.setNai(new IpAdjacencyBuilder().setLocalIpAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer))).build());
                break;
            case Ipv6Adjacency:
                srEroSubobjectBuilder.setNai(new IpAdjacencyBuilder().setLocalIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer))).build());
                break;
            case Unnumbered:
                srEroSubobjectBuilder.setNai(new UnnumberedAdjacencyBuilder().setLocalNodeId(buffer.readUnsignedInt())
                        .setLocalInterfaceId(buffer.readUnsignedInt()).setRemoteNodeId(buffer.readUnsignedInt())
                        .setRemoteInterfaceId(buffer.readUnsignedInt()).build());
                break;
            }
        }
        final SubobjectBuilder subobjectBuilder = new SubobjectBuilder();
        subobjectBuilder.setLoose(loose);
        subobjectBuilder.setSubobjectType(srEroSubobjectBuilder.build());
        return subobjectBuilder.build();
    }

    private int reverseBits(int in) {
        int out = 0;
        for (int ii = 0 ; ii < 8 ; ii++) {
            int bit = (in & 1);
            out = (out << 1) | bit;
            in = in >> 1;
        }
        return out;
    }

}