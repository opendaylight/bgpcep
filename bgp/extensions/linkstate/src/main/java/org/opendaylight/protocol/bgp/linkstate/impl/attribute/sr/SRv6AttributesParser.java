/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.srv6.Srv6EndXSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.srv6.Srv6EndXSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.srv6.Srv6LanEndXSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.srv6.Srv6LanEndXSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.Srv6Capabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.Srv6CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.Srv6Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.Srv6LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Srv6Sid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.flags.FlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.IsisNeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.IsisNeighborCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.Ospfv3NeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.lan.end.x.sid.neighbor.type.Ospfv3NeighborCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.sid.subtlvs.Srv6SidStructure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.srv6.sid.subtlvs.Srv6SidStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;

public final class SRv6AttributesParser {
    // SRv6 Capabilities flags
    private static final int CAP_FLAGS_SIZE = 16;
    private static final int O_BIT_FLAG = 1;

    // SRv6 Prefix and Link flags
    private static final int FLAGS_SIZE = 8;
    private static final int D_BIT_FLAG = 0;
    private static final int BACKUP_FLAG = 0 ;
    private static final int SET_FLAG = 1;
    private static final int PERSISTENT_FLAG = 2;

    // SRv6 SID subTLVs
    private static final int SRV6_SID_STRUCTURE = 1252;
    private static final int SRV6_SID_STRUCTURE_SIZE = 4;
    private static final int RESERVED_ONE = 1;
    private static final int RESERVED_TWO = 2;
    private static final int SRV6_SID_LENGTH = 16;
    private static final int ISO_SYSTEM_ID_SIZE = 6;

    private SRv6AttributesParser() {
        // Hidden on purpose
    }

    // Node Attibutes SRv6 Capabilities
    public static Srv6Capabilities parseSrv6Capabilities(final ByteBuf buffer) {
        final var flags = BitArray.valueOf(buffer, CAP_FLAGS_SIZE);
        return new Srv6CapabilitiesBuilder().setOFlag(flags.get(O_BIT_FLAG)).build();
    }

    public static void serialiseSrv6Capabilities(final Srv6Capabilities srv6Cap, final ByteBuf output) {
        final var bs = new BitArray(CAP_FLAGS_SIZE);
        bs.set(O_BIT_FLAG, srv6Cap.getOFlag());
        bs.toByteBuf(output);
        output.writeZero(RESERVED_TWO);
    }

    // Link Attributes SRv6 End X SID & LAN End X SID
    public static Srv6EndXSid parseSrv6EndXSid(final ByteBuf buffer) {
        final var sidBuilder = new Srv6EndXSidBuilder().setEndpointBehavior(readUint16(buffer));

        final var flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        sidBuilder
            .setFlags(new FlagsBuilder()
                .setBackup(flags.get(BACKUP_FLAG))
                .setPersistent(flags.get(PERSISTENT_FLAG))
                .setSet(flags.get(SET_FLAG))
                .build())
            .setAlgo(readUint8(buffer))
            .setWeight(readUint8(buffer));

        buffer.skipBytes(RESERVED_ONE);
        return sidBuilder
            .setSid(new Srv6Sid(ByteArray.readBytes(buffer, SRV6_SID_LENGTH)))
            .setSrv6SidStructure(parseSrv6SidStructure(buffer))
            .build();
    }

    public static void serialiseSrv6EndXSid(final Srv6EndXSid srv6EndXSid, final ByteBuf output) {
        writeUint16(output, srv6EndXSid.getEndpointBehavior());
        final var bs = new BitArray(FLAGS_SIZE);
        bs.set(BACKUP_FLAG, srv6EndXSid.getFlags().getBackup());
        bs.set(SET_FLAG, srv6EndXSid.getFlags().getSet());
        bs.set(PERSISTENT_FLAG, srv6EndXSid.getFlags().getPersistent());
        bs.toByteBuf(output);
        writeUint8(output, srv6EndXSid.getAlgo());
        writeUint8(output, srv6EndXSid.getWeight());
        output.writeZero(RESERVED_ONE);
        output.writeBytes(srv6EndXSid.getSid().getValue());
        serializeSrv6SidStructure(srv6EndXSid.getSrv6SidStructure(), output);
    }

    public static Srv6LanEndXSid parseSrv6LanEndXSid(final ByteBuf buffer, final ProtocolId protocolId) {
        final var sidBuilder = new Srv6LanEndXSidBuilder().setEndpointBehavior(readUint16(buffer));

        final var flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        sidBuilder.setFlags(new FlagsBuilder()
                .setBackup(flags.get(BACKUP_FLAG))
                .setSet(flags.get(SET_FLAG))
                .setPersistent(flags.get(PERSISTENT_FLAG))
                .build())
            .setAlgo(readUint8(buffer))
            .setWeight(readUint8(buffer));
        buffer.skipBytes(RESERVED_ONE);

        return sidBuilder
            .setNeighborType(switch (protocolId) {
                case IsisLevel1, IsisLevel2 ->
                    new IsisNeighborCaseBuilder()
                        .setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)))
                        .build();
                case OspfV3 ->
                    new Ospfv3NeighborCaseBuilder().setNeighborId(Ipv4Util.addressForByteBuf(buffer)).build();
                default -> null;
            })
            .setSid(new Srv6Sid(ByteArray.readBytes(buffer, SRV6_SID_LENGTH)))
            .setSrv6SidStructure(parseSrv6SidStructure(buffer))
            .build();
    }

    public static void serialiseSrv6LanEndXSid(final Srv6LanEndXSid srv6LanEndXSid, final ByteBuf output) {
        writeUint16(output, srv6LanEndXSid.getEndpointBehavior());

        final var bs = new BitArray(FLAGS_SIZE);
        bs.set(BACKUP_FLAG, srv6LanEndXSid.getFlags().getBackup());
        bs.set(SET_FLAG, srv6LanEndXSid.getFlags().getSet());
        bs.set(PERSISTENT_FLAG, srv6LanEndXSid.getFlags().getPersistent());
        bs.toByteBuf(output);

        writeUint8(output, srv6LanEndXSid.getAlgo());
        writeUint8(output, srv6LanEndXSid.getWeight());
        output.writeZero(RESERVED_ONE);

        switch (srv6LanEndXSid.getNeighborType()) {
            case IsisNeighborCase isis -> output.writeBytes(isis.getIsoSystemId().getValue());
            case Ospfv3NeighborCase ospf -> output.writeBytes(Ipv4Util.bytesForAddress(ospf.getNeighborId()));
            case null, default -> {
                // No-op
            }
        }

        output.writeBytes(srv6LanEndXSid.getSid().getValue());
        serializeSrv6SidStructure(srv6LanEndXSid.getSrv6SidStructure(), output);
    }

    private static Srv6SidStructure parseSrv6SidStructure(final ByteBuf buffer) {
        final int type = readUint16(buffer).intValue();
        final int length = readUint16(buffer).intValue();
        if (type != SRV6_SID_STRUCTURE || length != SRV6_SID_STRUCTURE_SIZE) {
            return null;
        }

        return new Srv6SidStructureBuilder()
            .setLocatorBlockLength(readUint8(buffer))
            .setLocatorNodeLength(readUint8(buffer))
            .setFunctionLength(readUint8(buffer))
            .setArgumentLength(readUint8(buffer))
            .build();
    }

    private static void serializeSrv6SidStructure(final Srv6SidStructure srv6SidStructure, final ByteBuf aggregator) {
        final ByteBuf output = Unpooled.buffer();
        writeUint8(output, srv6SidStructure.getLocatorBlockLength());
        writeUint8(output, srv6SidStructure.getLocatorNodeLength());
        writeUint8(output, srv6SidStructure.getFunctionLength());
        writeUint8(output, srv6SidStructure.getArgumentLength());
        TlvUtil.writeTLV(SRV6_SID_STRUCTURE, output, aggregator);
    }

    // Prefix Attributes SRv6 Locator
    public static Srv6Locator parseSrv6Locator(final ByteBuf buffer) {
        final var flags = BitArray.valueOf(buffer, FLAGS_SIZE);

        final var srv6LocatorBuilder = new Srv6LocatorBuilder()
            .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219
                .srv6.locator.FlagsBuilder().setUpDown(flags.get(D_BIT_FLAG)).build())
            .setAlgo(readUint8(buffer));
        buffer.skipBytes(RESERVED_TWO);

        return srv6LocatorBuilder.setMetric(readUint32(buffer)).build();
    }

    public static void serializeSrv6Locator(final Srv6Locator srv6Locator, final ByteBuf output) {
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(D_BIT_FLAG, srv6Locator.getFlags().getUpDown());
        bs.toByteBuf(output);
        writeUint8(output, srv6Locator.getAlgo());
        output.writeZero(RESERVED_TWO);
        writeUint32(output,  srv6Locator.getMetric());
    }
}
