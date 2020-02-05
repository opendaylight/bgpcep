/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;

public class NextHopAttributeParserTest {

    private static final byte[] IPV4_NEXT_HOP_BYTES = {
        (byte) 0x40, (byte) 0x03, (byte) 0x04, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };
    private static final byte[] IPV6_NEXT_HOP_BYTES = {
        (byte) 0x40, (byte) 0x03, (byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02
    };

    private static final Attributes IPV4_RESULT = new AttributesBuilder()
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                    .setGlobal(new Ipv4AddressNoZone("255.255.255.255"))
                    .build()).build()).build();
    private static final Attributes IPV6_RESULT = new AttributesBuilder()
            .setCNextHop(new Ipv6NextHopCaseBuilder().setIpv6NextHop(new Ipv6NextHopBuilder()
                    .setGlobal(new Ipv6AddressNoZone("ffff::1"))
                    .setLinkLocal(new Ipv6AddressNoZone("ffff::2"))
                    .build()).build()).build();

    @Test
    public void testIpv4AttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(IPV4_RESULT, actual);
        assertArrayEquals(IPV4_NEXT_HOP_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(IPV4_RESULT.getCNextHop(), attributeOut.getCNextHop());
    }

    @Test
    public void testIpv6AttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(IPV6_RESULT, actual);
        assertArrayEquals(IPV6_NEXT_HOP_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(IPV6_RESULT.getCNextHop(), attributeOut.getCNextHop());
    }

    @Test (expected = NullPointerException.class)
    public void testParseEmptyIpv4Attribute() {
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(new AttributesBuilder()
                        .setCNextHop(new Ipv4NextHopCaseBuilder().build())
                        .build(), Unpooled.buffer());
    }

    @Test (expected = NullPointerException.class)
    public void testParseEmptyIpv6Attribute() {
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(new AttributesBuilder()
                        .setCNextHop(new Ipv6NextHopCaseBuilder().build())
                        .build(), Unpooled.buffer());
    }
}
