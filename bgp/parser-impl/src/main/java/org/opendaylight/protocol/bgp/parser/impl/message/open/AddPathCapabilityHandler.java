/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;

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
        final CParameters1 aug = capability.augmentation(CParameters1.class);
        if (aug == null) {
            return;
        }
        final AddPathCapability addPathCap = aug.getAddPathCapability();
        if (addPathCap == null) {
            return;
        }

        final List<AddressFamilies> families = addPathCap.getAddressFamilies();
        if (families != null) {
            final ByteBuf capBuffer = Unpooled.buffer(families.size() * TRIPLET_BYTE_SIZE);
            for (final AddressFamilies addressFamily : families) {
                final AddressFamily afi = addressFamily.getAfi();
                final Integer afival = this.afiReg.numberForClass(afi);
                checkArgument(afival != null, "Unhandled address family " + afi);
                final SubsequentAddressFamily safi = addressFamily.getSafi();
                final Integer safival = this.safiReg.numberForClass(safi);
                checkArgument(safival != null, "Unhandled subsequent address family " + safi);
                final SendReceive sendReceive = addressFamily.getSendReceive();
                checkArgument(sendReceive != null, "Unhandled Send/Receive value");

                capBuffer.writeShort(afival).writeByte(safival).writeByte(sendReceive.getIntValue());
            }

            CapabilityUtil.formatCapability(CODE, capBuffer, byteAggregator);
        }
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final List<AddressFamilies> families = new ArrayList<>();
        while (buffer.isReadable()) {
            final int afiVal = buffer.readUnsignedShort();
            final AddressFamily afi = this.afiReg.classForFamily(afiVal);
            if (afi == null) {
                throw new BGPParsingException("Address Family Identifier: '" + afiVal + "' not supported.");
            }
            final int safiVal = buffer.readUnsignedByte();
            final SubsequentAddressFamily safi = this.safiReg.classForFamily(safiVal);
            if (safi == null) {
                throw new BGPParsingException("Subsequent Address Family Identifier: '" + safiVal + "' not supported.");
            }
            final SendReceive sendReceive = SendReceive.forValue(buffer.readUnsignedByte());
            if (sendReceive != null) {
                families.add(new AddressFamiliesBuilder().setAfi(afi).setSafi(safi).setSendReceive(sendReceive)
                    .build());
            }
        }
        return new CParametersBuilder().addAugmentation(new CParameters1Builder()
            .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(families).build()).build()).build();
    }

}
