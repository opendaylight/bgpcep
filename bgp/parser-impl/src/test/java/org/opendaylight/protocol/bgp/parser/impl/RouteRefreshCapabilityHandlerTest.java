/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.RouteRefreshCapabilityHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.RouteRefreshCapabilityBuilder;

public class RouteRefreshCapabilityHandlerTest {

    private static final RouteRefreshCapabilityHandler HANDLER = new RouteRefreshCapabilityHandler();

    private static final byte[] WRONG_BYTES = new byte[] {1, 2};
    private static final byte[] OK_BYTES = new byte[] {0};
    private static final byte[] CAP_BYTES = new byte[] {2, 0};

    @Test
    public void testRRCapHandler() throws BGPDocumentedException, BGPParsingException {
        final CParameters expectedParams = new CParametersBuilder().addAugmentation(CParameters1.class,new CParameters1Builder().setRouteRefreshCapability(
            new RouteRefreshCapabilityBuilder().build()).build()).build();
        assertEquals(expectedParams, HANDLER.parseCapability(Unpooled.copiedBuffer(OK_BYTES)));
        assertEquals(expectedParams, HANDLER.parseCapability(Unpooled.copiedBuffer(WRONG_BYTES)));

        final ByteBuf byteAggregator = Unpooled.buffer(2);
        HANDLER.serializeCapability(expectedParams, byteAggregator);
        assertEquals(Unpooled.copiedBuffer(CAP_BYTES), byteAggregator);

        final CParameters missingCap = new CParametersBuilder().addAugmentation(CParameters1.class,new CParameters1Builder().setRouteRefreshCapability(
            null).build()).build();
        final ByteBuf byteAggregator2 = Unpooled.buffer(0);
        HANDLER.serializeCapability(missingCap, byteAggregator2);
        assertEquals(Unpooled.copiedBuffer(new byte[]{}), byteAggregator2);
    }
}
