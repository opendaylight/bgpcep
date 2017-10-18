/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

public final class BgpPrefixSidAttributeParserTest {

    private final BgpPrefixSidTlvRegistry reg = Mockito.mock(BgpPrefixSidTlvRegistry.class);
    private final BgpPrefixSidTlv tlv = Mockito.mock(BgpPrefixSidTlv.class);
    private final BgpPrefixSidAttributeParser parser = new BgpPrefixSidAttributeParser(this.reg);
    private final byte[] bytes = new byte[] {1, 2, 3};

    @Before
    public void setUp() {
        Mockito.doReturn(this.tlv).when(this.reg).parseBgpPrefixSidTlv(Mockito.anyInt(), Mockito.any(ByteBuf.class));
        Mockito.doNothing().when(this.reg).serializeBgpPrefixSidTlv(Mockito.any(BgpPrefixSidTlv.class), Mockito.any(ByteBuf.class));
    }

    @Test
    public void testHandling() throws BGPDocumentedException, BGPParsingException {
        final AttributesBuilder builder = new AttributesBuilder();
        this.parser.parseAttribute(Unpooled.copiedBuffer(this.bytes), builder);
        assertEquals(3, builder.getBgpPrefixSid().getBgpPrefixSidTlvs().size());

        this.parser.serializeAttribute(builder.build(), Unpooled.EMPTY_BUFFER);
        Mockito.verify(this.reg, Mockito.times(3)).serializeBgpPrefixSidTlv(Mockito.any(BgpPrefixSidTlv.class), Mockito.any(ByteBuf.class));
    }
}
