/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public final class MultiProtocolCapabilityHandler implements CapabilityParser, CapabilitySerializer {
    public static final int CODE = 1;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public MultiProtocolCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = Preconditions.checkNotNull(afiReg);
        this.safiReg = Preconditions.checkNotNull(safiReg);
    }

    @Override
    public MultiprotocolCase parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final int afiVal = buffer.readUnsignedShort();
        final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afiVal);
        if (afi == null) {
            throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
        }
        // skip reserved
        buffer.skipBytes(1);
        final int safiVal = UnsignedBytes.toInt(buffer.readByte());
        final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safiVal);
        if (safi == null) {
            throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
        }
        return new MultiprotocolCaseBuilder().setMultiprotocolCapability(
                new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build();
    }

    @Override
    public byte[] serializeCapability(final CParameters capability) {
        final MultiprotocolCase mp = (MultiprotocolCase) capability;

        final Class<? extends AddressFamily> afi = mp.getMultiprotocolCapability().getAfi();
        final Integer afival = this.afiReg.numberForClass(afi);
        Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);

        final Class<? extends SubsequentAddressFamily> safi = mp.getMultiprotocolCapability().getSafi();
        final Integer safival = this.safiReg.numberForClass(safi);
        Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);

        return CapabilityUtil.formatCapability(CODE, new byte[] { UnsignedBytes.checkedCast(afival / 256),
            UnsignedBytes.checkedCast(afival % 256), 0, UnsignedBytes.checkedCast(safival) });
    }
}