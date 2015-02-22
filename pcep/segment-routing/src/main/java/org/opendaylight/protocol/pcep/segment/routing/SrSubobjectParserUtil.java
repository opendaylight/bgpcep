/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeBitSet;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.Nai;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.IpAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.IpAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.UnnumberedAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.UnnumberedAdjacencyBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

final class SrSubobjectParserUtil {

    public static final int MINIMAL_LENGTH = 4;
    public static final int BITSET_LENGTH = 8;

    private static final int FLAGS_OFFSET = 1;
    private static final int SID_TYPE_BITS_OFFSET = 4;

    private SrSubobjectParserUtil() {
        throw new UnsupportedOperationException();
    }

    public static ByteBuf serializeSrSubobject(final SrSubobject srSubobject, final BitSet bits) {
        Preconditions.checkArgument(srSubobject.getNai() != null || srSubobject.getSid() != null,
                "Both SID and NAI are absent in SR subobject.");
        final ByteBuf body = Unpooled.buffer(MINIMAL_LENGTH);
        writeUnsignedByte((short)(srSubobject.getSidType().getIntValue() << SID_TYPE_BITS_OFFSET), body);

        writeBitSet(bits, FLAGS_OFFSET, body);

        if (srSubobject.getSid() != null) {
            writeUnsignedInt(srSubobject.getSid(), body);
        }
        final Nai nai = srSubobject.getNai();
        if (nai != null) {
            switch (srSubobject.getSidType()) {
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
            default:
                break;
            }
        }
        return body;
    }

    public static SrSubobject parseSrSubobject(final ByteBuf buffer, final Function<BitSet, Void> getFlags, final int fPosition, final int sPosition)
            throws PCEPDeserializerException {
        final int sidTypeByte = buffer.readByte() >> SID_TYPE_BITS_OFFSET;
        final SidType sidType = SidType.forValue(sidTypeByte);

        final BitSet bitSet = ByteArray.bytesToBitSet(new byte[] { buffer.readByte() });
        getFlags.apply(bitSet);
        final boolean f = bitSet.get(fPosition);
        final boolean s = bitSet.get(sPosition);

        if (f && s) {
            throw new PCEPDeserializerException("Both SID and NAI are absent in SR subobject.");
        }
        final Long sid;
        if (!s) {
            sid = buffer.readUnsignedInt();
        } else {
            sid = null;
        }

        final Nai nai;

        if (sidType != null && !f) {
            switch (sidType) {
            case Ipv4NodeId:
                nai = new IpNodeIdBuilder().setIpAddress(
                        new IpAddress(new Ipv4Address(Ipv4Util.addressForByteBuf(buffer)))).build();
                break;
            case Ipv6NodeId:
                nai = new IpNodeIdBuilder().setIpAddress(
                        new IpAddress(Ipv6Util.addressForByteBuf(buffer))).build();
                break;
            case Ipv4Adjacency:
                nai = new IpAdjacencyBuilder()
                        .setLocalIpAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddress(Ipv4Util.addressForByteBuf(buffer))).build();
                break;
            case Ipv6Adjacency:
                nai = new IpAdjacencyBuilder()
                        .setLocalIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddress(Ipv6Util.addressForByteBuf(buffer))).build();
                break;
            case Unnumbered:
                nai = new UnnumberedAdjacencyBuilder().setLocalNodeId(buffer.readUnsignedInt())
                        .setLocalInterfaceId(buffer.readUnsignedInt()).setRemoteNodeId(buffer.readUnsignedInt())
                        .setRemoteInterfaceId(buffer.readUnsignedInt()).build();
                break;
            default:
                nai = null;
                break;
            }
        } else {
            nai = null;
        }

        return new SrSubobject() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return SrSubobject.class;
            }
            @Override
            public SidType getSidType() {
                return sidType;
            }
            @Override
            public Long getSid() {
                return sid;
            }
            @Override
            public Nai getNai() {
                return nai;
            }
        };
    }
}