/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import static java.util.Objects.requireNonNull;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.AddPathCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public class AddPathCapabilityHandler implements CapabilityParser, CapabilitySerializer {

    public static final int CODE = 69;
    private static final int TRIPLET_BYTE_SIZE = 4;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public AddPathCapabilityHandler(final AddressFamilyRegistry afiReg, final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = requireNonNull(afiReg);
        this.safiReg = requireNonNull(safiReg);
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        if ( (capability.getAugmentation(CParameters1.class) == null)
            || (capability.getAugmentation(CParameters1.class).getAddPathCapability() == null) ) {
            return;
        }
        final AddPathCapability addPathCap = capability.getAugmentation(CParameters1.class).getAddPathCapability();

        final List<AddressFamilies> families = addPathCap.getAddressFamilies();
        if (families != null) {
            final ByteBuf capBuffer = Unpooled.buffer(families.size() * TRIPLET_BYTE_SIZE);
            for (final AddressFamilies addressFamily : families) {
                final Class<? extends AddressFamily> afi = addressFamily.getAfi();
                final Integer afival = this.afiReg.numberForClass(afi);
                Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);
                capBuffer.writeShort(afival);

                final Class<? extends SubsequentAddressFamily> safi = addressFamily.getSafi();
                final Integer safival = this.safiReg.numberForClass(safi);
                Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);
                capBuffer.writeByte(safival);

                final SendReceive sendReceive = addressFamily.getSendReceive();
                Preconditions.checkArgument(sendReceive != null, "Unhandled Send/Receive value");
                capBuffer.writeByte(sendReceive.getIntValue());
            }

            CapabilityUtil.formatCapability(CODE, capBuffer, byteAggregator);
        }
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final List<AddressFamilies> families = new ArrayList<>();
        while (buffer.isReadable()) {
            final int afiVal = buffer.readUnsignedShort();
            final Class<? extends AddressFamily> afi = this.afiReg.classForFamily(afiVal);
            if (afi == null) {
                throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
            }
            final int safiVal = buffer.readUnsignedByte();
            final Class<? extends SubsequentAddressFamily> safi = this.safiReg.classForFamily(safiVal);
            if (safi == null) {
                throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
            }
            final SendReceive sendReceive = SendReceive.forValue(buffer.readUnsignedByte());
            if (sendReceive != null) {
                families.add(new AddressFamiliesBuilder().setAfi(afi).setSafi(safi).setSendReceive(sendReceive).build());
            }
        }
        return new CParametersBuilder().addAugmentation(CParameters1.class,new CParameters1Builder().setAddPathCapability(
            new AddPathCapabilityBuilder().setAddressFamilies(families).build()).build()).build();
    }

}
