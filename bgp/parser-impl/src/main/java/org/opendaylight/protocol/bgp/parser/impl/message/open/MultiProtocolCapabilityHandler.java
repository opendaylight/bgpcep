/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;

public final class MultiProtocolCapabilityHandler implements CapabilityParser, CapabilitySerializer {

    public static final int CODE = 1;

    private final AddressFamilyRegistry afiReg;
    private final SubsequentAddressFamilyRegistry safiReg;

    public MultiProtocolCapabilityHandler(final AddressFamilyRegistry afiReg,
            final SubsequentAddressFamilyRegistry safiReg) {
        this.afiReg = requireNonNull(afiReg);
        this.safiReg = requireNonNull(safiReg);
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final Optional<BgpTableType> parsedAfiSafiOptional = MultiprotocolCapabilitiesUtil
                .parseMPAfiSafi(buffer, this.afiReg, this.safiReg);
        return parsedAfiSafiOptional.map(bgpTableType -> new CParametersBuilder()
                .addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
                new MultiprotocolCapabilityBuilder(bgpTableType).build()).build()).build()).orElse(null);
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        final CParameters1 aug = capability.augmentation(CParameters1.class);
        if (aug == null) {
            return;
        }
        final MultiprotocolCapability mp = aug.getMultiprotocolCapability();
        if (mp == null) {
            return;
        }

        final ByteBuf capBuffer = Unpooled.buffer();
        MultiprotocolCapabilitiesUtil.serializeMPAfiSafi(this.afiReg, this.safiReg, mp.getAfi(),
                mp.getSafi(), capBuffer);

        CapabilityUtil.formatCapability(CODE, capBuffer, byteAggregator);
    }

}