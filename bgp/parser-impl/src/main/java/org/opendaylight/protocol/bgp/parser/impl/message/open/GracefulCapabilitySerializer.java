package org.opendaylight.protocol.bgp.parser.impl.message.open;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;


/**
 * Created by cgasparini on 7.5.2015.
 */
public final class GracefulCapabilitySerializer implements CapabilitySerializer {
    // Restart timer size, in bits
    private static final int TIMER_SIZE = 12;
    private static final int MAX_RESTART_TIME = 4095;
    // Restart flag size, in bits
    private static final int RESTART_FLAG_STATE = 0x8000;
    // Size of the capability header
    private static final int HEADER_SIZE = (GracefulCapabilityHandler.RESTART_FLAGS_SIZE + TIMER_SIZE) / Byte.SIZE;
    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public GracefulCapabilitySerializer(AddressFamilyRegistry afiReg, SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    @Override
    public void serializeCapability(CParameters capability, ByteBuf byteAggregator) {
        Preconditions.checkArgument(capability.getAugmentation(CParameters1.class) != null, "Augmentation is null");
        Preconditions.checkArgument(capability.getAugmentation(CParameters1.class).getGracefulRestartCapability() != null, "GracefulRestartCapability is null");
        final GracefulRestartCapability grace = capability.getAugmentation(CParameters1.class).getGracefulRestartCapability();
        final List<Tables> tables = grace.getTables();
        final int tablesSize = (tables != null) ? tables.size() : 0;
        final ByteBuf bytes = Unpooled.buffer(HEADER_SIZE + (GracefulCapabilityHandler.PER_AFI_SAFI_SIZE *
            tablesSize));

        int timeval = 0;
        Integer time = grace.getRestartTime();
        if ( time == null ) {
            time = 0;
        }
        Preconditions.checkArgument(time >= 0 && time <= MAX_RESTART_TIME, "Restart time is " + time);
        timeval = time;
        final GracefulRestartCapability.RestartFlags flags = grace.getRestartFlags();
        if ( flags != null && flags.isRestartState() ) {
            writeUnsignedShort(RESTART_FLAG_STATE | timeval, bytes);
        } else {
            writeUnsignedShort(timeval, bytes);
        }

        if ( tables != null ) {
            for (final Tables t : tables) {
                final Class<? extends AddressFamily> afi = t.getAfi();
                final Integer afival = this.afiReg.numberForClass(afi);
                Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);
                bytes.writeShort(afival);

                final Class<? extends SubsequentAddressFamily> safi = t.getSafi();
                final Integer safival = this.safiReg.numberForClass(safi);
                Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);
                bytes.writeByte(safival);

                if ( t.getAfiFlags() != null && t.getAfiFlags().isForwardingState() ) {
                    bytes.writeByte(GracefulCapabilityHandler.AFI_FLAG_FORWARDING_STATE);
                } else {
                    bytes.writeZero(1);
                }
            }
        }
        CapabilityUtil.formatCapability(GracefulCapabilityHandler.CODE, bytes, byteAggregator);
    }
}
