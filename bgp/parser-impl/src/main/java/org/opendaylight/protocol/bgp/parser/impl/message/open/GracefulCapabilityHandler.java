/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart._case.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GracefulCapabilityHandler implements CapabilityParser, CapabilitySerializer {
    public static final int CODE = 64;

    private static final Logger LOG = LoggerFactory.getLogger(GracefulCapabilityHandler.class);

    // Restart flag size, in bits
    private static final int RESTART_FLAGS_SIZE = 4;
    private static final int RESTART_FLAG_STATE = 0x8000;

    // Restart timer size, in bits
    private static final int TIMER_SIZE = 12;
    private static final int TIMER_TOPBITS_MASK = 0x0F;

    // Size of the capability header
    private static final int HEADER_SIZE = (RESTART_FLAGS_SIZE + TIMER_SIZE) / Byte.SIZE;

    // Length of each AFI/SAFI array member, in bytes
    private static final int PER_AFI_SAFI_SIZE = 4;

    private static final short AFI_FLAG_FORWARDING_STATE = 0x80;

    private static final int MAX_RESTART_TIME = 4095;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public GracefulCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(capability instanceof GracefulRestartCase);
        final GracefulRestartCapability grace = ((GracefulRestartCase) capability).getGracefulRestartCapability();
        final List<Tables> tables = grace.getTables();
        final int tablesSize  = (tables != null) ? tables.size() : 0;
        final ByteBuf bytes = Unpooled.buffer(HEADER_SIZE + (PER_AFI_SAFI_SIZE * tablesSize));

        int timeval = 0;
        Integer time = grace.getRestartTime();
        if (time == null) {
            time = 0;
        }
        Preconditions.checkArgument(time >= 0 && time <= MAX_RESTART_TIME, "Restart time is %s", time);
        timeval = time;
        final RestartFlags flags = grace.getRestartFlags();
        if (flags != null && flags.isRestartState()) {
            writeUnsignedShort(RESTART_FLAG_STATE | timeval, bytes);
        } else {
            writeUnsignedShort(timeval, bytes);
        }

        if (tables != null) {
            for (final Tables t : tables) {
                final Class<? extends AddressFamily> afi = t.getAfi();
                final Integer afival = this.afiReg.numberForClass(afi);
                Preconditions.checkArgument(afival != null, "Unhandled address family %s", afi);
                bytes.writeShort(afival);

                final Class<? extends SubsequentAddressFamily> safi = t.getSafi();
                final Integer safival = this.safiReg.numberForClass(safi);
                Preconditions.checkArgument(safival != null, "Unhandled subsequent address family %s", safi);
                bytes.writeByte(safival);

                if (t.getAfiFlags() != null && t.getAfiFlags().isForwardingState()) {
                    bytes.writeByte(AFI_FLAG_FORWARDING_STATE);
                } else {
                    bytes.writeZero(1);
                }
            }
        }
        CapabilityUtil.formatCapability(CODE, bytes, byteAggregator);
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final GracefulRestartCapabilityBuilder cb = new GracefulRestartCapabilityBuilder();

        final int flagBits = (buffer.getByte(0) >> RESTART_FLAGS_SIZE);
        cb.setRestartFlags(new RestartFlags((flagBits & Byte.SIZE) != 0));

        final int timer = ((buffer.readUnsignedByte() & TIMER_TOPBITS_MASK) << RESTART_FLAGS_SIZE) + buffer.readUnsignedByte();
        cb.setRestartTime(timer);

        final List<Tables> tables = new ArrayList<>();
        while (buffer.readableBytes() != 0) {
            final int afiVal = buffer.readShort();
            final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afiVal);
            if (afi == null) {
                LOG.debug("Ignoring GR capability for unknown address family {}", afiVal);
                buffer.skipBytes(PER_AFI_SAFI_SIZE - 2);
                continue;
            }
            final int safiVal = buffer.readUnsignedByte();
            final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safiVal);
            if (safi == null) {
                LOG.debug("Ignoring GR capability for unknown subsequent address family {}", safiVal);
                buffer.skipBytes(1);
                continue;
            }
            final int flags = buffer.readUnsignedByte();
            tables.add(new TablesBuilder().setAfi(afi).setSafi(safi).setAfiFlags(new AfiFlags((flags & AFI_FLAG_FORWARDING_STATE) != 0)).build());
        }
        cb.setTables(tables);
        return new GracefulRestartCaseBuilder().setGracefulRestartCapability(cb.build()).build();
    }
}
