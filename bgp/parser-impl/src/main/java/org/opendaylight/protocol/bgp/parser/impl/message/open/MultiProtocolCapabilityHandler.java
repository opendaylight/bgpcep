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
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public final class MultiProtocolCapabilityHandler implements CapabilityParser, CapabilitySerializer {
    public static final int CODE = 1;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    private static final int RESERVED = 1;

    public MultiProtocolCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final int afiVal = buffer.readUnsignedShort();
        final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afiVal);
        if (afi == null) {
            throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
        }
        // skip reserved
        buffer.skipBytes(RESERVED);
        final int safiVal = buffer.readUnsignedByte();
        final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safiVal);
        if (safi == null) {
            throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
        }

        return new CParametersBuilder().addAugmentation(CParameters1.class,new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build()).build();
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        if ( capability.getAugmentation(CParameters1.class) == null
            || capability.getAugmentation(CParameters1.class).getMultiprotocolCapability() == null ) {
            return;
        }
        final MultiprotocolCapability mp = capability.getAugmentation(CParameters1.class).getMultiprotocolCapability();

        final ByteBuf capBuffer = Unpooled.buffer();
        final Class<? extends AddressFamily> afi = mp.getAfi();
        final Integer afival = this.afiReg.numberForClass(afi);
        Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);
        capBuffer.writeShort(afival);

        final Class<? extends SubsequentAddressFamily> safi = mp.getSafi();
        final Integer safival = this.safiReg.numberForClass(safi);
        Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);
        capBuffer.writeZero(RESERVED);
        capBuffer.writeByte(safival);

        CapabilityUtil.formatCapability(CODE, capBuffer, byteAggregator);
    }
}