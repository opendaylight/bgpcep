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
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;
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
        final BindingMap.Builder<TablesKey, Tables> tables = BindingMap.builder();

        while (buffer.isReadable()) {
            final short afival = buffer.readShort();
            final AddressFamily afi = this.afiReg.classForFamily(afival);
            if (afi == null) {
                LOG.debug("Ignoring GR capability for unknown address family {}", afival);
                buffer.skipBytes(PER_TABLE_SIZE - 2);
                continue;
            }

            final byte safival = buffer.readByte();
            final SubsequentAddressFamily safi = this.safiReg.classForFamily(safival);
            if (safi == null) {
                LOG.debug("Ignoring GR capability for unknown subsequent address family {}", safival);
                buffer.skipBytes(PER_TABLE_SIZE - 3);
                continue;
            }

            final byte afiFlags = buffer.readByte();
            final int staleTime = buffer.readUnsignedMedium();
            final Tables table = new TablesBuilder()
                    .setAfi(afi)
                    .setSafi(safi)
                    .setAfiFlags(new Tables.AfiFlags(afiFlags == AFI_FLAG_FORWARDING_STATE))
                    .setLongLivedStaleTime(new Uint24(Uint32.valueOf(staleTime)))
                    .build();
            tables.add(table);
        }
        return new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setLlGracefulRestartCapability(new LlGracefulRestartCapabilityBuilder()
                        .setTables(tables.build())
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
        final Map<TablesKey, Tables> tables = cap.getTables();
        if (tables == null || tables.isEmpty()) {
            return Unpooled.EMPTY_BUFFER;
        }

        final ByteBuf buffer = Unpooled.buffer(PER_TABLE_SIZE * tables.size());
        for (Tables table : tables.values()) {
            final AddressFamily afi = table.getAfi();
            final Integer afival = this.afiReg.numberForClass(afi);
            checkArgument(afival != null, "Unhandled address family %s", afi);
            buffer.writeShort(afival);
            final SubsequentAddressFamily safi = table.getSafi();
            final Integer safival = this.safiReg.numberForClass(safi);
            checkArgument(safival != null, "Unhandled subsequent address family %s", safi);
            buffer.writeByte(safival);
            if (table.getAfiFlags() != null && table.getAfiFlags().getForwardingState()) {
                buffer.writeByte(AFI_FLAG_FORWARDING_STATE);
            } else {
                buffer.writeByte(0);
            }
            final Uint24 staleTime = table.getLongLivedStaleTime();
            final int timeval = staleTime != null ? staleTime.getValue().intValue() : 0;
            checkArgument(timeval >= 0 && timeval <= MAX_STALE_TIME, "Restart time is %s", staleTime);
            buffer.writeMedium(timeval);
        }
        return buffer;
    }
}
