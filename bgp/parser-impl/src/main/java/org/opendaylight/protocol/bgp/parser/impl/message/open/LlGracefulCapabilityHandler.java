/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LlGracefulCapabilityHandler implements CapabilityParser, CapabilitySerializer {
    public static final int CODE = 71;

    private static final Logger LOG = LoggerFactory.getLogger(LlGracefulCapabilityHandler.class);

    // size of AFI (16 bits) and SAFI (8bits) fields in bytes
    private static final int AFI_SAFI_SIZE = 3;

    private static final int AFI_FLAGS_SIZE = 1;
    private static final int STALE_TIME_SIZE = 3;

    private static final int PER_TABLE_SIZE = AFI_SAFI_SIZE + AFI_FLAGS_SIZE + STALE_TIME_SIZE;

    private static final int MAX_STALE_TIME = 16777215;
    private static final byte AFI_FLAG_FORWARDING_STATE = (byte) 0x80;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public LlGracefulCapabilityHandler(final AddressFamilyRegistry afiReg,
                                       final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = requireNonNull(afiReg);
        this.safiReg = requireNonNull(safiReg);
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) {
        final List<Tables> tables = new ArrayList<>();

        while (buffer.isReadable()) {
            final short afival = buffer.readShort();
            final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afival);
            if (afi == null) {
                LOG.debug("Ignoring GR capability for unknown address family {}", afival);
                buffer.skipBytes(PER_TABLE_SIZE - 2);
                continue;
            }

            final byte safival = buffer.readByte();
            final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safival);
            if (safi == null) {
                LOG.debug("Ignoring GR capability for unknown subsequent address family {}", safival);
                buffer.skipBytes(1);
                continue;
            }

            final byte afiFlags = buffer.readByte();
            final int staleTime = buffer.readUnsignedMedium();
            final Tables table = new TablesBuilder()
                    .setAfi(afi)
                    .setSafi(safi)
                    .setAfiFlags(new Tables.AfiFlags(Boolean.valueOf(afiFlags == AFI_FLAG_FORWARDING_STATE)))
                    .setLongLiveStaleTime(Long.valueOf(staleTime))
                    .build();
            tables.add(table);
        }
        return new CParametersBuilder()
                .addAugmentation(CParameters1.class, new CParameters1Builder()
                        .setLlGracefulRestartCapability(new LlGracefulRestartCapabilityBuilder()
                                .setTables(tables)
                                .build())
                        .build())
                .build();
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        final CParameters1 aug = capability.augmentation(CParameters1.class);
        if (aug != null) {
            final LlGracefulRestartCapability cap = aug.getLlGracefulRestartCapability();
            if (cap != null) {
                CapabilityUtil.formatCapability(CODE, serializeCapability(cap), byteAggregator);
            }
        }
    }

    private ByteBuf serializeCapability(final LlGracefulRestartCapability cap) {
        final List<Tables> tables = cap.getTables();
        if (tables == null || tables.isEmpty()) {
            return Unpooled.EMPTY_BUFFER;
        }

        final ByteBuf buffer = Unpooled.buffer(PER_TABLE_SIZE * tables.size());
        for (Tables table : tables) {
            final Class<? extends AddressFamily> afi = table.getAfi();
            final Class<? extends SubsequentAddressFamily> safi = table.getSafi();
            final Integer afival = this.afiReg.numberForClass(afi);
            checkArgument(afival != null, "Unhandled address family %s", afi);
            buffer.writeShort(afival);
            final Integer safival = this.safiReg.numberForClass(safi);
            checkArgument(safival != null, "Unhandled subsequent address family %s", safi);
            buffer.writeByte(safival);
            if (table.getAfiFlags() != null && table.getAfiFlags().isForwardingState()) {
                buffer.writeByte(AFI_FLAG_FORWARDING_STATE);
            } else {
                buffer.writeZero(1);
            }
            final Long staleTime = table.getLongLiveStaleTime();
            final int timeval = staleTime != null ? staleTime.intValue() : 0;
            checkArgument(timeval >= 0 && timeval <= MAX_STALE_TIME, "Restart time is %s", staleTime);
            buffer.writeMedium(timeval);
        }
        return buffer;
    }
}
