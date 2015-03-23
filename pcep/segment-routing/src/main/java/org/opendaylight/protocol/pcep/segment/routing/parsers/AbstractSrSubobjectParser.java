package org.opendaylight.protocol.pcep.segment.routing.parsers;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv6Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedByte;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
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

public abstract class AbstractSrSubobjectParser   {

    private static final int MINIMAL_LENGTH = 4;
    private static final int BITSET_LENGTH = 8;
    private static final int M_FLAG_POSITION = 7;
    private static final int C_FLAG_POSITION = 6;
    private static final int S_FLAG_POSITION = 5;
    private static final int F_FLAG_POSITION = 4;
    private static final int MPLS_LABEL_OFFSET = 12;
    private static final int SID_TYPE_BITS_OFFSET = 4;


    public ByteBuf serializeSubobject(final SrSubobject srSubobject) {
        final ByteBuf buffer = Unpooled.buffer(MINIMAL_LENGTH);
        // sid type
        writeUnsignedByte((short)(srSubobject.getSidType().getIntValue() << SID_TYPE_BITS_OFFSET), buffer);

        final BitArray bits = new BitArray(BITSET_LENGTH);
        bits.set(M_FLAG_POSITION, srSubobject.isMFlag());
        bits.set(C_FLAG_POSITION, srSubobject.isCFlag());
        if (srSubobject.getSid() == null) {
            bits.set(S_FLAG_POSITION, Boolean.TRUE);
        }
        if (srSubobject.getNai() == null) {
            bits.set(F_FLAG_POSITION, Boolean.TRUE);
        }
        // bits
        bits.toByteBuf(buffer);
        // sid
        Preconditions.checkArgument(srSubobject.getNai() != null && srSubobject.getSid() != null,
                "Both SID and NAI are absent in SR subobject.");
        if (srSubobject.getSid() != null) {
            if (srSubobject.isCFlag() && srSubobject.isMFlag()) {
                writeUnsignedInt(srSubobject.getSid() >> MPLS_LABEL_OFFSET, buffer);
            }
            writeUnsignedInt(srSubobject.getSid(), buffer);
        }
        // nai
        final Nai nai = srSubobject.getNai();
        if (nai != null) {
            switch (srSubobject.getSidType()) {
            case Ipv4NodeId:
                writeIpv4Address(((IpNodeId) nai).getIpAddress().getIpv4Address(), buffer);
                break;
            case Ipv6NodeId:
                writeIpv6Address(((IpNodeId) nai).getIpAddress().getIpv6Address(), buffer);
                break;
            case Ipv4Adjacency:
                writeIpv4Address(((IpAdjacency) nai).getLocalIpAddress().getIpv4Address(), buffer);
                writeIpv4Address(((IpAdjacency) nai).getRemoteIpAddress().getIpv4Address(), buffer);
                break;
            case Ipv6Adjacency:
                writeIpv6Address(((IpAdjacency) nai).getLocalIpAddress().getIpv6Address(), buffer);
                writeIpv6Address(((IpAdjacency) nai).getRemoteIpAddress().getIpv6Address(), buffer);
                break;
            case Unnumbered:
                final UnnumberedAdjacency unnumbered = (UnnumberedAdjacency) nai;
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getLocalNodeId(), buffer);
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getLocalInterfaceId(), buffer);
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getRemoteNodeId(), buffer);
                ByteBufWriteUtil.writeUnsignedInt(unnumbered.getRemoteInterfaceId(), buffer);
                break;
            default:
                break;
            }
        }
        return buffer;
    }


    protected final SrSubobject parseSrSubobject(final ByteBuf buffer)
            throws PCEPDeserializerException {
        final int sidTypeByte = buffer.readByte() >> SID_TYPE_BITS_OFFSET;
        final SidType sidType = SidType.forValue(sidTypeByte);

        final BitArray bitSet = BitArray.valueOf(buffer.readByte());
        final boolean f = bitSet.get(F_FLAG_POSITION);
        final boolean s = bitSet.get(S_FLAG_POSITION);
        final boolean c = bitSet.get(C_FLAG_POSITION);
        final boolean m = bitSet.get(M_FLAG_POSITION);

        if (f && s) {
            throw new PCEPDeserializerException("Both SID and NAI are absent in SR subobject.");
        }

        Long sid_pom;
        if (!s) {
            sid_pom = buffer.readUnsignedInt();
            if (c && m) {
                sid_pom = sid_pom >> MPLS_LABEL_OFFSET;
            }
        } else {
            sid_pom = null;
        }
        final Long sid = sid_pom;
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
            public Boolean isMFlag() {
                return m;
            }

            @Override
            public Boolean isCFlag() {
                return c;
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
