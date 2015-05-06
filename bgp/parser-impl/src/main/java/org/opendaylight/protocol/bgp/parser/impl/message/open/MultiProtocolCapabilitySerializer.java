package org.opendaylight.protocol.bgp.parser.impl.message.open;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

/**
 * Created by cgasparini on 7.5.2015.
 */
public final class MultiProtocolCapabilitySerializer implements CapabilitySerializer {
    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public MultiProtocolCapabilitySerializer(AddressFamilyRegistry afiReg, SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    @Override
    public void serializeCapability(CParameters capability, ByteBuf byteAggregator) {
        Preconditions.checkArgument(capability.getAugmentation(CParameters1.class) != null, "Augmentation is null");
        Preconditions.checkArgument(capability.getAugmentation(CParameters1.class).getMultiprotocolCapability() != null, "MultiprotocolCapability is null");
        final MultiprotocolCapability mp = capability.getAugmentation(CParameters1.class).getMultiprotocolCapability();
        final ByteBuf capBuffer = Unpooled.buffer();
        final Class<? extends AddressFamily> afi = mp.getAfi();
        final Integer afival = this.afiReg.numberForClass(afi);
        Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);
        capBuffer.writeShort(afival);

        final Class<? extends SubsequentAddressFamily> safi = mp.getSafi();
        final Integer safival = this.safiReg.numberForClass(safi);
        Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);
        capBuffer.writeZero(MultiProtocolCapabilityHandler.RESERVED);
        capBuffer.writeByte(safival);

        CapabilityUtil.formatCapability(MultiProtocolCapabilityHandler.CODE, capBuffer, byteAggregator);

    }
}
