/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBitSet;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
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

    private static final int FLAGS_OFFSET = 1;
    private static final int SID_TYPE_BITS_OFFSET = 4;
    private static final int MINIMAL_LENGTH = 4;

    private static final int M_FLAG_POSITION = 7;
    private static final int C_FLAG_POSITION = 6;
    private static final int S_FLAG_POSITION = 5;
    private static final int F_FLAG_POSITION = 4;

    @Override
    public void serializeSubobject(final Subobject subobject, final ByteBuf buffer) {
        Preconditions.checkArgument(subobject.getSubobjectType() instanceof SrEroSubobject,
                "Unknown subobject instance. Passed %s. Needed SrEroSubobject.", subobject.getSubobjectType()
                        .getClass());

        final SrEroSubobject srEroSubobject = (SrEroSubobject) subobject.getSubobjectType();
        final ByteBuf body = Unpooled.buffer(MINIMAL_LENGTH);
        writeUnsignedByte((short)(srEroSubobject.getSidType().getIntValue() << SID_TYPE_BITS_OFFSET), body);

        final Flags flags = srEroSubobject.getFlags();
        final BitSet bits = new BitSet();
        if (flags != null) {
            bits.set(M_FLAG_POSITION, flags.isM());
            bits.set(C_FLAG_POSITION, flags.isC());
            bits.set(S_FLAG_POSITION, flags.isS());
            bits.set(F_FLAG_POSITION, flags.isF());
        }
        writeBitSet(bits, FLAGS_OFFSET, body);

        if (srEroSubobject.getSid() != null && !flags.isS()) {
            long sid = srEroSubobject.getSid();
            if (flags.isM()) {
                sid = sid << 12;
            }
            writeUnsignedInt(sid, body);
        }
        final Nai nai = srEroSubobject.getNai();
        if (nai != null && !flags.isF()) {
            switch (srEroSubobject.getSidType()) {
            case Ipv4NodeId:
                writeIpv4Address(((IpNodeId) nai).getIpAddress().getIpv4Address(), body);
                break;
            case Ipv6NodeId:
                writeIpv6Address(((IpNodeId) nai).getIpAddress().getIpv6Address(), body);
                break;
            case Ipv4Adjacency:
                writeIpv4Address(((IpAdjacency) nai).getLocalIpAddress().getIpv4Address(), body);
                writeIpv4Address(((IpAdjacency) nai).getRemoteIpAddress().getIpv4Address(), body);
                break;
            case Ipv6Adjacency:
                writeIpv6Address(((IpAdjacency) nai).getLocalIpAddress().getIpv6Address(), body);
                writeIpv6Address(((IpAdjacency) nai).getRemoteIpAddress().getIpv6Address(), body);
                break;
            case Unnumbered:
                final UnnumberedAdjacency unnumbered = (UnnumberedAdjacency) nai;
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getLocalNodeId(), body);
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getLocalInterfaceId(), body);
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getRemoteNodeId(), body);
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getRemoteInterfaceId(), body);
                break;
            }
        }
        EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), body, buffer);
    }

    @Override
    public Subobject parseSubobject(final ByteBuf buffer, final boolean loose) throws PCEPDeserializerException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() <= MINIMAL_LENGTH) {
            throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");
        }

        final SrEroTypeBuilder srEroSubobjectBuilder = new SrEroTypeBuilder();
        final int sidTypeByte = buffer.readByte() >> SID_TYPE_BITS_OFFSET;
        final SidType sidType = SidType.forValue(sidTypeByte);
        srEroSubobjectBuilder.setSidType(sidType);

        final BitSet bitSet = ByteArray.bytesToBitSet(new byte[] { buffer.readByte() });
        final boolean f = bitSet.get(F_FLAG_POSITION);
        final boolean s = bitSet.get(S_FLAG_POSITION);
        final boolean c = bitSet.get(C_FLAG_POSITION);
        final boolean m = bitSet.get(M_FLAG_POSITION);
        final Flags flags = new Flags(c, f, m, s);
        srEroSubobjectBuilder.setFlags(flags);

        if (!flags.isS()) {
            Long sid = buffer.readUnsignedInt();
            if (flags.isM()) {
                sid = sid >> 12;
            }
            srEroSubobjectBuilder.setSid(sid);
        }
        if (sidType != null && !flags.isF()) {
            switch (sidType) {
            case Ipv4NodeId:
                srEroSubobjectBuilder.setNai(new IpNodeIdBuilder().setIpAddress(
                        new IpAddress(new Ipv4Address(Ipv4Util.addressForByteBuf(buffer)))).build());
                break;
            case Ipv6NodeId:
                srEroSubobjectBuilder.setNai(new IpNodeIdBuilder().setIpAddress(
                        new IpAddress(Ipv6Util.addressForByteBuf(buffer))).build());
                break;
            case Ipv4Adjacency:
                srEroSubobjectBuilder.setNai(new IpAdjacencyBuilder()
                        .setLocalIpAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer))).build());
                break;
            case Ipv6Adjacency:
                srEroSubobjectBuilder.setNai(new IpAdjacencyBuilder()
                        .setLocalIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer)))
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

}