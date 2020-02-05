/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

public final class BgpPrefixSidAttributeParserTest {

    private final BgpPrefixSidTlvRegistry reg = mock(BgpPrefixSidTlvRegistry.class);
    private final BgpPrefixSidTlv tlv = mock(BgpPrefixSidTlv.class);
    private final BgpPrefixSidAttributeParser parser = new BgpPrefixSidAttributeParser(this.reg);
    private final byte[] bytes = new byte[] {1, 2, 3};

    @Before
    public void setUp() {
        doReturn(this.tlv).when(this.reg).parseBgpPrefixSidTlv(anyInt(), any(ByteBuf.class));
        doNothing().when(this.reg).serializeBgpPrefixSidTlv(any(BgpPrefixSidTlv.class), any(ByteBuf.class));
    }

    @Test
    public void testHandling() throws BGPDocumentedException, BGPParsingException {
        final AttributesBuilder builder = new AttributesBuilder();
        this.parser.parseAttribute(Unpooled.copiedBuffer(this.bytes), builder, null);
        assertEquals(3, builder.getBgpPrefixSid().getBgpPrefixSidTlvs().size());

        this.parser.serializeAttribute(builder.build(), Unpooled.EMPTY_BUFFER);
        verify(this.reg, times(3)).serializeBgpPrefixSidTlv(any(BgpPrefixSidTlv.class), any(ByteBuf.class));
    }
}
