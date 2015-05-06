/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;


import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GracefulCapabilityHandler implements CapabilityParser{
    public static final int CODE = 64;

    private static final Logger LOG = LoggerFactory.getLogger(GracefulCapabilityHandler.class);

    // Restart flag size, in bits
    public static final int RESTART_FLAGS_SIZE = 4;

    // Restart timer size, in bits
    private static final int TIMER_TOPBITS_MASK = 0x0F;

    // Length of each AFI/SAFI array member, in bytes
    public static final int PER_AFI_SAFI_SIZE = 4;
    public static final short AFI_FLAG_FORWARDING_STATE = 0x80;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public GracefulCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
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

        return new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setGracefulRestartCapability(cb.build()).build()).build();
    }
}
