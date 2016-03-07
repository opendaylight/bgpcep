/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.RouteRefreshCapabilityBuilder;

public class RouteRefreshCapabilityHandler implements CapabilityParser, CapabilitySerializer {

    // https://tools.ietf.org/html/rfc2918#section-2
    public static final int CODE = 2;
    public static final int LENGTH_FIELD = 1;

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        if ( (capability.getAugmentation(CParameters1.class) == null) ||
            (capability.getAugmentation(CParameters1.class).getRouteRefreshCapability() == null) ) {
            return;
        }
        byteAggregator.writeByte(CODE);
        byteAggregator.writeZero(LENGTH_FIELD);
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        if (!buffer.isReadable()) {
            return null;
        }
        // read zero length
        buffer.readUnsignedByte();
        return new CParametersBuilder().addAugmentation(CParameters1.class,new CParameters1Builder().setRouteRefreshCapability(
            new RouteRefreshCapabilityBuilder().build()).build()).build();
    }

}
