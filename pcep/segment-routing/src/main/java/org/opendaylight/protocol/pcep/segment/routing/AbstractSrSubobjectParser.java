/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.NaiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.Nai;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.IpAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.IpAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.Ipv6Local;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.Ipv6LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.UnnumberedAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.UnnumberedAdjacencyBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractSrSubobjectParser {
    protected static final int MINIMAL_LENGTH = 4;
    protected static final int BITSET_LENGTH = 8;
    protected static final int M_FLAG_POSITION = 7;
    protected static final int C_FLAG_POSITION = 6;
    protected static final int S_FLAG_POSITION = 5;
    protected static final int F_FLAG_POSITION = 4;
    protected static final int MPLS_LABEL_OFFSET = 12;

    private static final int NAI_TYPE_BITS_OFFSET = 4;

    private static class SrSubobjectImpl implements SrSubobject {
        private final boolean mflag;
        private final boolean cflag;
        private final NaiType naiType;
        private final Uint32 sid;
        private final Nai nai;

        SrSubobjectImpl(final boolean mflag, final boolean cflag, final NaiType naiType, final Uint32 sid,
                final Nai nai) {
            this.mflag = mflag;
            this.cflag = cflag;
            this.naiType = naiType;
            this.sid = sid;
            this.nai = nai;
        }

        @Override
        public Class<SrSubobject> implementedInterface() {
            return SrSubobject.class;
        }

        @Override
        public Boolean getMFlag() {
            return this.mflag;
        }

        @Override
        public Boolean getCFlag() {
            return this.cflag;
        }

        @Override
        public NaiType getNaiType() {
            return this.naiType;
        }

        @Override
        public Uint32 getSid() {
            return this.sid;
        }

        @Override
        public Nai getNai() {
            return this.nai;
        }
    }

    public ByteBuf serializeSubobject(final SrSubobject srSubobject) {
        checkArgument(srSubobject.getNai() != null || srSubobject.getSid() != null,
                "Both SID and NAI are absent in SR subobject.");

        final ByteBuf buffer = Unpooled.buffer(MINIMAL_LENGTH);

        /* Write NAI Type */
        buffer.writeByte(srSubobject.getNaiType().getIntValue() << NAI_TYPE_BITS_OFFSET);

        /* Flags set according to RFC8664#section 4.3.1 */
        final BitArray bits = new BitArray(BITSET_LENGTH);
        bits.set(M_FLAG_POSITION, srSubobject.getMFlag());
        /* C flag MUST be set to 0 if M flag is set to 0 */
        if (!srSubobject.getMFlag()) {
            bits.set(C_FLAG_POSITION, Boolean.FALSE);
        } else {
            bits.set(C_FLAG_POSITION, srSubobject.getCFlag());
        }
        /* M & C flags MUST be set to 0 if S flag is set to 1 */
        if (srSubobject.getSid() == null) {
            bits.set(M_FLAG_POSITION, Boolean.FALSE);
            bits.set(C_FLAG_POSITION, Boolean.FALSE);
            bits.set(S_FLAG_POSITION, Boolean.TRUE);
        }
        /* F flag MUST be set if NT=0 or NAI is absent */
        if (srSubobject.getNai() == null || srSubobject.getNaiType().getIntValue() == 0) {
            bits.set(F_FLAG_POSITION, Boolean.TRUE);
        }
        /* Write Flags */
        bits.toByteBuf(buffer);

        /* Write SID */
        if (srSubobject.getSid() != null) {
            if (srSubobject.getMFlag()) {
                buffer.writeInt(srSubobject.getSid().intValue() << MPLS_LABEL_OFFSET);
            } else {
                ByteBufUtils.writeOrZero(buffer, srSubobject.getSid());
            }
        }

        /* Write NAI */
        final Nai nai = srSubobject.getNai();
        if (nai != null) {
            serializeNai(nai, srSubobject.getNaiType(), buffer);
        }
        return buffer;
    }

    private static void serializeNai(final Nai nai, final NaiType naiType, final ByteBuf buffer) {
        switch (naiType) {
            case Ipv4NodeId:
                Ipv4Util.writeIpv4Address(((IpNodeId) nai).getIpAddress().getIpv4AddressNoZone(), buffer);
                break;
            case Ipv6NodeId:
                Ipv6Util.writeIpv6Address(((IpNodeId) nai).getIpAddress().getIpv6AddressNoZone(), buffer);
                break;
            case Ipv4Adjacency:
                Ipv4Util.writeIpv4Address(((IpAdjacency) nai).getLocalIpAddress().getIpv4AddressNoZone(), buffer);
                Ipv4Util.writeIpv4Address(((IpAdjacency) nai).getRemoteIpAddress().getIpv4AddressNoZone(), buffer);
                break;
            case Ipv6Adjacency:
                Ipv6Util.writeIpv6Address(((IpAdjacency) nai).getLocalIpAddress().getIpv6AddressNoZone(), buffer);
                Ipv6Util.writeIpv6Address(((IpAdjacency) nai).getRemoteIpAddress().getIpv6AddressNoZone(), buffer);
                break;
            case Unnumbered:
                final UnnumberedAdjacency unnumbered = (UnnumberedAdjacency) nai;
                ByteBufUtils.writeOrZero(buffer, unnumbered.getLocalNodeId());
                ByteBufUtils.writeOrZero(buffer, unnumbered.getLocalInterfaceId());
                ByteBufUtils.writeOrZero(buffer, unnumbered.getRemoteNodeId());
                ByteBufUtils.writeOrZero(buffer, unnumbered.getRemoteInterfaceId());
                break;
            case Ipv6Local:
                final Ipv6Local ipv6Local = (Ipv6Local) nai;
                Ipv6Util.writeIpv6Address(ipv6Local.getLocalIpv6Address(), buffer);
                ByteBufUtils.writeOrZero(buffer, ipv6Local.getLocalId());
                Ipv6Util.writeIpv6Address(ipv6Local.getRemoteIpv6Address(), buffer);
                ByteBufUtils.writeOrZero(buffer, ipv6Local.getRemoteId());
                break;
            default:
                break;
        }
    }

    private static Nai parseNai(final NaiType naiType, final ByteBuf buffer) {
        switch (naiType) {
            case Ipv4NodeId:
                return new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer)))
                        .build();
            case Ipv6NodeId:
                return new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer)))
                        .build();
            case Ipv4Adjacency:
                return new IpAdjacencyBuilder()
                        .setLocalIpAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer))).build();
            case Ipv6Adjacency:
                return new IpAdjacencyBuilder()
                        .setLocalIpAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer)))
                        .setRemoteIpAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer))).build();
            case Unnumbered:
                return new UnnumberedAdjacencyBuilder()
                        .setLocalNodeId(ByteBufUtils.readUint32(buffer))
                        .setLocalInterfaceId(ByteBufUtils.readUint32(buffer))
                        .setRemoteNodeId(ByteBufUtils.readUint32(buffer))
                        .setRemoteInterfaceId(ByteBufUtils.readUint32(buffer)).build();
            case Ipv6Local:
                return new Ipv6LocalBuilder()
                        .setLocalIpv6Address(Ipv6Util.addressForByteBuf(buffer))
                        .setLocalId(ByteBufUtils.readUint32(buffer))
                        .setRemoteIpv6Address(Ipv6Util.addressForByteBuf(buffer))
                        .setRemoteId(ByteBufUtils.readUint32(buffer)).build();
            default:
                return null;
        }
    }

    protected static SrSubobject parseSrSubobject(final ByteBuf buffer) throws PCEPDeserializerException {
        final int naiTypeByte = buffer.readByte() >> NAI_TYPE_BITS_OFFSET;
        final NaiType naiType = NaiType.forValue(naiTypeByte);
        final BitArray bitSet = BitArray.valueOf(buffer.readByte());
        final boolean f = bitSet.get(F_FLAG_POSITION);
        final boolean s = bitSet.get(S_FLAG_POSITION);
        final boolean c = bitSet.get(C_FLAG_POSITION);
        final boolean m = bitSet.get(M_FLAG_POSITION);

        if (f && s) {
            throw new PCEPDeserializerException("Both SID and NAI are absent in SR subobject.");
        }

        final Uint32 sid;
        if (!s) {
            final long tmp = buffer.readUnsignedInt();
            sid = Uint32.valueOf(m ? tmp >> MPLS_LABEL_OFFSET : tmp);
        } else {
            sid = null;
        }
        final Nai nai;
        if (naiType != null && naiType.getIntValue() != 0 && !f) {
            nai = parseNai(naiType, buffer);
        } else {
            nai = null;
        }
        return new SrSubobjectImpl(m, c, naiType, sid, nai);
    }
}
