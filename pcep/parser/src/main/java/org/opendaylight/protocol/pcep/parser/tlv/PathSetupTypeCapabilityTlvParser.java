/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.tlv;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.capability.tlv.PathSetupTypeCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.capability.tlv.PathSetupTypeCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.Srv6PceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.srv6.pce.capability.Msds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.srv6.pce.capability.tlv.srv6.pce.capability.MsdsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class PathSetupTypeCapabilityTlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 34;
    private static final int SRV6_PCE = 27;
    private static final int SR_PCE = 26;

    private static final int OFFSET = 2;
    private static final int RESERVED = 3;
    private static final int SR_BITSET_LENGTH = 8;
    private static final int SR_N_FLAG_POSITION = 6;
    private static final int SR_X_FLAG_POSITION = 7;
    private static final int SRV6_BITSET_LENGTH = 16;
    private static final int SRV6_N_FLAG_POSITION = 14;

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof PathSetupTypeCapability, "PathSetupTypeCapability is mandatory.");
        final PathSetupTypeCapability pstCapability = (PathSetupTypeCapability) tlv;
        final ByteBuf body = Unpooled.buffer();
        body.writeZero(RESERVED);
        body.writeByte(pstCapability.getPsts().size());
        pstCapability.getPsts().forEach(pst -> body.writeByte(pst.getIntValue()));
        // Write padding if necessary
        body.writeZero(TlvUtil.getPadding(pstCapability.getPsts().size(), TlvUtil.PADDED_TO));
        if (pstCapability.getSrPceCapability() != null) {
            serializeSrPceCapability(pstCapability.getSrPceCapability(), body);
        }
        if (pstCapability.getSrv6PceCapability() != null) {
            serializeSrv6PceCapability(pstCapability.getSrv6PceCapability(), body);
        }
        TlvUtil.formatTlv(TYPE, body, buffer);
    }

    @Override
    public Tlv parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        buffer.skipBytes(RESERVED);
        final int numPST = buffer.readByte();
        final var psts = new ArrayList<PsType>();
        for (int i = 0; i < numPST; i++) {
            final var psType = PsType.forValue(buffer.readByte());
            if (psType != null) {
                psts.add(psType);
            }
        }
        // Skip padding if any
        buffer.skipBytes(TlvUtil.getPadding(numPST, TlvUtil.PADDED_TO));
        final PathSetupTypeCapabilityBuilder pstcBuilder = new PathSetupTypeCapabilityBuilder().setPsts(psts);
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            switch (type) {
                case SR_PCE -> pstcBuilder.setSrPceCapability(parseSrPceCapability(buffer, psts));
                case SRV6_PCE -> pstcBuilder.setSrv6PceCapability(parseSrv6PceCapability(buffer, psts, length));
                default -> buffer.skipBytes(length);
            }
            // Skip padding if any
            buffer.skipBytes(TlvUtil.getPadding(TlvUtil.HEADER_SIZE + length, TlvUtil.PADDED_TO));
        }
        return pstcBuilder.build();
    }

    private void serializeSrPceCapability(final SrPceCapability srPceCapability, final ByteBuf buffer) {
        final ByteBuf body = Unpooled.buffer();

        /* Reserved 2 bytes */
        body.writerIndex(OFFSET);

        /* Flags */
        final BitArray bits = new BitArray(SR_BITSET_LENGTH);
        bits.set(SR_N_FLAG_POSITION, srPceCapability.getNFlag());
        bits.set(SR_X_FLAG_POSITION, srPceCapability.getXFlag());
        bits.toByteBuf(body);

        /* MSD */
        ByteBufUtils.writeOrZero(body, srPceCapability.getMsd());

        TlvUtil.formatTlv(SR_PCE, body, buffer);
    }

    private SrPceCapability parseSrPceCapability(final ByteBuf buffer, final List<PsType> psts)
            throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (!psts.contains(PsType.SrMpls)) {
            throw new PCEPDeserializerException("Missing SR-MPLS in Path Setup Type list.");
        }
        // Skip Reserved
        buffer.skipBytes(OFFSET);
        final BitArray bitSet = BitArray.valueOf(buffer, SR_BITSET_LENGTH);
        final boolean n = bitSet.get(SR_N_FLAG_POSITION);
        final boolean x = bitSet.get(SR_X_FLAG_POSITION);

        return new SrPceCapabilityBuilder()
            .setNFlag(n)
            .setXFlag(x)
            .setMsd(ByteBufUtils.readUint8(buffer))
            .build();
    }

    public void serializeSrv6PceCapability(final Srv6PceCapability srv6PceCapability, final ByteBuf buffer) {
        final ByteBuf body = Unpooled.buffer();

        /* Reserved 2 bytes */
        body.writeZero(OFFSET);

        /* Flags */
        final BitArray bits = new BitArray(SRV6_BITSET_LENGTH);
        bits.set(SRV6_N_FLAG_POSITION, srv6PceCapability.getNFlag());
        bits.toByteBuf(body);

        /* MSDs */
        if (srv6PceCapability.getMsds() != null) {
            srv6PceCapability.getMsds().forEach(msd -> {
                ByteBufUtils.writeUint8(body, msd.getMsdType());
                ByteBufUtils.writeUint8(body, msd.getMsdValue());
            });
        }

        TlvUtil.formatTlv(SRV6_PCE, body, buffer);
    }

    public Srv6PceCapability parseSrv6PceCapability(final ByteBuf buffer, final List<PsType> psts, final int length)
            throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (!psts.contains(PsType.Srv6)) {
            throw new PCEPDeserializerException("Missing SRv6 in Path Setup Type list.");
        }
        // Skip reserved
        buffer.skipBytes(OFFSET);
        final Srv6PceCapabilityBuilder srv6PceCapa = new Srv6PceCapabilityBuilder()
            .setNFlag(BitArray.valueOf(buffer, SRV6_BITSET_LENGTH).get(SRV6_N_FLAG_POSITION));

        /* MSDs */
        final List<Msds> msds = new ArrayList<Msds>();
        for (int i = 0; i < (length - TlvUtil.HEADER_SIZE) / 2; i++) {
            final var type = ByteBufUtils.readUint8(buffer);
            // Check if Type / Value is valid i.e. not padding
            if (type != Uint8.ZERO) {
                msds.add(new MsdsBuilder().setMsdType(type).setMsdValue(ByteBufUtils.readUint8(buffer)).build());
            } else {
                buffer.skipBytes(1);
            }
        }
        if (!msds.isEmpty()) {
            srv6PceCapa.setMsds(msds);
        }

        return srv6PceCapa.build();
    }
}
