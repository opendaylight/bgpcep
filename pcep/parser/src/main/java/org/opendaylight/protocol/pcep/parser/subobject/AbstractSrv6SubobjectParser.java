/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.subobject;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Srv6Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.SidStructure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.SidStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.Srv6Nai;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.srv6.nai.Ipv6Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.srv6.nai.Ipv6AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.srv6.nai.Ipv6Local;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.srv6.nai.Ipv6LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.srv6.nai.Ipv6NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srv6.subobject.srv6.nai.Ipv6NodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.NaiType;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractSrv6SubobjectParser {
    protected static final int BITSET_LENGTH = 8;
    protected static final int S_FLAG_POSITION = 7;
    protected static final int F_FLAG_POSITION = 6;
    protected static final int T_FLAG_POSITION = 5;
    protected static final int V_FLAG_POSITION = 4;

    private static final int NAI_TYPE_BITS_OFFSET = 4;
    private static final int RESERVED = 2;
    private static final int MIN_LENGTH = 22;

    private static class Srv6SubobjectImpl implements Srv6Subobject {
        private final boolean vflag;
        private final NaiType naiType;
        private final Ipv6AddressNoZone srv6Sid;
        private final SidStructure sidStructure;
        private final Uint16 endpointBehavior;
        private final Srv6Nai srv6Nai;

        Srv6SubobjectImpl(final boolean vflag, final NaiType naiType, final Ipv6AddressNoZone srv6Sid,
                final Srv6Nai srv6Nai, final Uint16 endpointBehavior, final SidStructure sidStructure) {
            this.vflag = vflag;
            this.naiType = naiType;
            this.srv6Sid = srv6Sid;
            this.srv6Nai = srv6Nai;
            this.endpointBehavior = endpointBehavior;
            this.sidStructure = sidStructure;
        }

        @Override
        public Class<SrSubobject> implementedInterface() {
            return SrSubobject.class;
        }

        @Override
        public Boolean getVFlag() {
            return vflag;
        }

        @Override
        public NaiType getSrv6NaiType() {
            return naiType;
        }

        @Override
        public Ipv6AddressNoZone getSrv6Sid() {
            return srv6Sid;
        }

        @Override
        public Srv6Nai getSrv6Nai() {
            return srv6Nai;
        }

        @Override
        public Uint16 getEndpointBehavior() {
            return endpointBehavior;
        }

        @Override
        public SidStructure getSidStructure() {
            return sidStructure;
        }

        @Override
        public @NonNull SidStructure nonnullSidStructure() {
            return sidStructure;
        }
    }

    public ByteBuf serializeSubobject(final Srv6Subobject srv6Subobject) {
        checkArgument(srv6Subobject.getSrv6Nai() != null || srv6Subobject.getSrv6Sid() != null,
                "Both SID and NAI are absent in SRv6 subobject.");

        final ByteBuf buffer = Unpooled.buffer();

        /* Write NAI Type */
        buffer.writeByte(srv6Subobject.getSrv6NaiType().getIntValue() << NAI_TYPE_BITS_OFFSET);

        /* Flags set according to RFC9603#section 4.3.1 */
        final BitArray bits = new BitArray(BITSET_LENGTH);
        bits.set(V_FLAG_POSITION, srv6Subobject.getVFlag());
        /* F & T flags MUST be set to 0 if S flag is set to 1 i.e there is no SRv6-SID */
        if (srv6Subobject.getSrv6Sid() == null) {
            bits.set(F_FLAG_POSITION, Boolean.FALSE);
            bits.set(T_FLAG_POSITION, Boolean.FALSE);
            bits.set(S_FLAG_POSITION, Boolean.TRUE);
        } else {
            /* S flag MUST be set to 0 if F flag is set to 1 i.e. SRv6-SID is present */
            bits.set(F_FLAG_POSITION, Boolean.TRUE);
            bits.set(S_FLAG_POSITION, Boolean.FALSE);
            /* T flag MUST be set to 1 if Sid Structure is present */
            if (srv6Subobject.getSidStructure() != null) {
                bits.set(T_FLAG_POSITION, Boolean.TRUE);
            } else {
                bits.set(T_FLAG_POSITION, Boolean.FALSE);
            }
        }
        /* F flag MUST be set if NT=0 or NAI is absent */
        if (srv6Subobject.getSrv6Nai() == null || srv6Subobject.getSrv6NaiType().getIntValue() == 0) {
            bits.set(F_FLAG_POSITION, Boolean.TRUE);
        }
        /* Write Flags */
        bits.toByteBuf(buffer);

        /* Write Reserved + Endpoint Behavior */
        buffer.writeZero(RESERVED);
        ByteBufUtils.writeOrZero(buffer, srv6Subobject.getEndpointBehavior());

        /* Write Srv6-SID */
        if (srv6Subobject.getSrv6Sid() != null) {
            Ipv6Util.writeIpv6Address(srv6Subobject.getSrv6Sid(), buffer);
            if (srv6Subobject.getSidStructure() != null) {
                ByteBufUtils.writeUint8(buffer, srv6Subobject.getSidStructure().getLocatorBlockLength());
                ByteBufUtils.writeUint8(buffer, srv6Subobject.getSidStructure().getLocatorNodeLength());
                ByteBufUtils.writeUint8(buffer, srv6Subobject.getSidStructure().getFunctionLength());
                ByteBufUtils.writeUint8(buffer, srv6Subobject.getSidStructure().getArgumentLength());
                ByteBufUtils.writeUint32(buffer, Uint32.ZERO);
            }
        }

        /* Write NAI */
        final Srv6Nai srv6Nai = srv6Subobject.getSrv6Nai();
        if (srv6Nai != null) {
            serializeSrv6Nai(srv6Nai, srv6Subobject.getSrv6NaiType(), buffer);
        }
        return buffer;
    }

    private static void serializeSrv6Nai(final Srv6Nai srv6Nai, final NaiType naiType, final ByteBuf buffer) {
        switch (naiType) {
            case Ipv6NodeId ->
                Ipv6Util.writeIpv6Address(((Ipv6NodeId) srv6Nai).getIpv6Address(), buffer);
            case Ipv6Adjacency -> {
                Ipv6Util.writeIpv6Address(((Ipv6Adjacency) srv6Nai).getIpv6LocalAddress(), buffer);
                Ipv6Util.writeIpv6Address(((Ipv6Adjacency) srv6Nai).getIpv6RemoteAddress(), buffer);
            }
            case Ipv6Local -> {
                final Ipv6Local ipv6Local = (Ipv6Local) srv6Nai;
                Ipv6Util.writeIpv6Address(ipv6Local.getLocalIpv6(), buffer);
                ByteBufUtils.writeOrZero(buffer, ipv6Local.getLocalIdentifier());
                Ipv6Util.writeIpv6Address(ipv6Local.getRemoteIpv6(), buffer);
                ByteBufUtils.writeOrZero(buffer, ipv6Local.getRemoteIdentifier());
            }
            default -> {
                // no-op
            }
        }
    }

    private static Srv6Nai parseSrv6Nai(final NaiType naiType, final ByteBuf buffer) {
        return switch (naiType) {
            case Ipv6NodeId -> new Ipv6NodeIdBuilder()
                .setIpv6Address(new Ipv6AddressNoZone(Ipv6Util.addressForByteBuf(buffer)))
                .build();
            case Ipv6Adjacency -> new Ipv6AdjacencyBuilder()
                .setIpv6LocalAddress(new Ipv6AddressNoZone(Ipv6Util.addressForByteBuf(buffer)))
                .setIpv6RemoteAddress(new Ipv6AddressNoZone(Ipv6Util.addressForByteBuf(buffer)))
                .build();
            case Ipv6Local -> new Ipv6LocalBuilder()
                .setLocalIpv6(Ipv6Util.addressForByteBuf(buffer))
                .setLocalIdentifier(ByteBufUtils.readUint32(buffer))
                .setRemoteIpv6(Ipv6Util.addressForByteBuf(buffer))
                .setRemoteIdentifier(ByteBufUtils.readUint32(buffer))
                .build();
            default -> null;
        };
    }

    protected static Srv6Subobject parseSrv6Subobject(final ByteBuf buffer) throws PCEPDeserializerException {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < MIN_LENGTH) {
            throw new PCEPDeserializerException("Buffer too small: expected at least: " + MIN_LENGTH + " but got: "
                + buffer.readableBytes());
        }
        final int naiTypeByte = buffer.readByte() >> NAI_TYPE_BITS_OFFSET;
        final NaiType naiType = NaiType.forValue(naiTypeByte);
        final BitArray bitSet = BitArray.valueOf(buffer.readByte());
        final boolean v = bitSet.get(V_FLAG_POSITION);
        final boolean t = bitSet.get(T_FLAG_POSITION);
        final boolean f = bitSet.get(F_FLAG_POSITION);
        final boolean s = bitSet.get(S_FLAG_POSITION);

        if (f && s) {
            throw new PCEPDeserializerException("Both Srv6-SID and NAI are absent in SRv6 subobject.");
        }

        buffer.skipBytes(RESERVED);
        final Uint16 endpointBehavior = ByteBufUtils.readUint16(buffer);
        Ipv6AddressNoZone srv6Sid = null;
        SidStructure sidStructure = null;
        if (!s) {
            srv6Sid = new Ipv6AddressNoZone(Ipv6Util.addressForByteBuf(buffer));
            if (t) {
                sidStructure = new SidStructureBuilder()
                    .setLocatorBlockLength(ByteBufUtils.readUint8(buffer))
                    .setLocatorNodeLength(ByteBufUtils.readUint8(buffer))
                    .setFunctionLength(ByteBufUtils.readUint8(buffer))
                    .setArgumentLength(ByteBufUtils.readUint8(buffer))
                    .build();
                // Read Reserved and Flags which are not defined but present.
                ByteBufUtils.readUint32(buffer);
            }
        }
        final Srv6Nai srv6Nai;
        if (naiType != null && naiType.getIntValue() != 0 && !f) {
            srv6Nai = parseSrv6Nai(naiType, buffer);
        } else {
            srv6Nai = null;
        }
        return new Srv6SubobjectImpl(v, naiType, srv6Sid, srv6Nai, endpointBehavior, sidStructure);
    }
}
