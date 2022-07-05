/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.RouteRefreshCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MultiprotocolCapabilitiesUtil {
    public static final CParameters RR_CAPABILITY = new CParametersBuilder()
            .addAugmentation(new CParameters1Builder()
                .setRouteRefreshCapability(new RouteRefreshCapabilityBuilder().build())
                .build())
            .build();

    private static final Logger LOG = LoggerFactory.getLogger(MultiprotocolCapabilitiesUtil.class);
    private static final int RESERVED = 1;

    private MultiprotocolCapabilitiesUtil() {
        // Hidden on purpose
    }

    public static void serializeMPAfiSafi(final AddressFamilyRegistry afiReg,
            final SubsequentAddressFamilyRegistry safiReg, final AddressFamily afi, final SubsequentAddressFamily safi,
            final ByteBuf capBuffer) {
        final Integer afival = afiReg.numberForClass(afi);
        Preconditions.checkArgument(afival != null, "Unhandled address family " + afi);
        capBuffer.writeShort(afival);

        capBuffer.writeZero(RESERVED);

        final Integer safival = safiReg.numberForClass(safi);
        Preconditions.checkArgument(safival != null, "Unhandled subsequent address family " + safi);
        capBuffer.writeByte(safival);
    }

    public static Optional<BgpTableType> parseMPAfiSafi(final ByteBuf buffer, final AddressFamilyRegistry afiReg,
            final SubsequentAddressFamilyRegistry safiReg) {
        final int afiVal = buffer.readUnsignedShort();
        final AddressFamily afi = afiReg.classForFamily(afiVal);
        if (afi == null) {
            LOG.info("Unsupported AFI {} parsed.", afiVal);
            return Optional.empty();
        }
        // skip reserved
        buffer.skipBytes(RESERVED);
        final int safiVal = buffer.readUnsignedByte();
        final SubsequentAddressFamily safi = safiReg.classForFamily(safiVal);
        if (safi == null) {
            LOG.info("Unsupported SAFI {} parsed.", safiVal);
            return Optional.empty();
        }
        return Optional.of(new BgpTableTypeImpl(afi, safi));
    }
}
