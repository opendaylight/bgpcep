/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;

public class RouteRefreshCapabilityHandler implements CapabilityParser, CapabilitySerializer {

    // https://tools.ietf.org/html/rfc2918#section-2
    public static final int CODE = 2;

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf byteAggregator) {
        if (capability == null || (capability.getAugmentation(CParameters1.class) == null) ||
            (capability.getAugmentation(CParameters1.class).getRouteRefreshCapability() == null) ) {
            return;
        }
        CapabilityUtil.formatCapability(CODE, Unpooled.EMPTY_BUFFER, byteAggregator);
    }

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        return MultiprotocolCapabilitiesUtil.RR_CAPABILITY;
    }

}
