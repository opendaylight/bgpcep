/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.BgpExtendedMessageCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.BgpExtendedMessageCapabilityBuilder;

public class BgpExtendedMessageCapabilityHandlerTest {

    private BGPExtensionProviderContext ctx;

    @Before
    public void setUp() {
        this.ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
    }

    @Test
    public void testBgpExtendedMessageCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final BgpExtendedMessageCapabilityHandler handler = new BgpExtendedMessageCapabilityHandler();

        final byte[] bgpExeBytes = {(byte) 0x06, (byte) 0x00};
        final BgpExtendedMessageCapabilityBuilder bgpExtBuilder = new BgpExtendedMessageCapabilityBuilder();

        final ByteBuf buffer = Unpooled.buffer(bgpExeBytes.length);
        handler.serializeCapability( new CParametersBuilder().setBgpExtendedMessageCapability(bgpExtBuilder.build()).build(), buffer);
        Assert.assertArrayEquals(bgpExeBytes, buffer.array());
        Assert.assertEquals(handler.parseCapability(Unpooled.wrappedBuffer(bgpExeBytes)), new CParametersBuilder().setBgpExtendedMessageCapability(bgpExtBuilder.build()).build());

        final byte[] bgpExeBytes2 = {(byte) 0x40, (byte) 0x06};
        buffer.clear();
        handler.serializeCapability(new CParametersBuilder().setBgpExtendedMessageCapability(bgpExtBuilder.build()).build(), buffer);
        Assert.assertNotSame(bgpExeBytes2, buffer.array());
    }

}
