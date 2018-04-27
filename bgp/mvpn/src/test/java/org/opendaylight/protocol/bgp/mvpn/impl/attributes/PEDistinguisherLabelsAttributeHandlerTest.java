/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mvpn.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.PeDistinguisherLabelsAttributeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.PeDistinguisherLabelsAttributeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.PeDistinguisherLabelsAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.pe.distinguisher.labels.attribute.PeDistinguisherLabelAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.pe.distinguisher.labels.attribute.pe.distinguisher.labels.attribute.PeDistinguisherLabelAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

public class PEDistinguisherLabelsAttributeHandlerTest {
    /**
     * ATT - TYPE - ATT LENGTH.
     * PE ADDRESS - MPLS LABEL
     * PE ADDRESS - MPLS LABEL
     */
    private static final byte[] PE_DISTINGUISHER_LABELS = {
        (byte) 0xC0, (byte) 0x1B, (byte) 0x0e,
        (byte) 0x7F, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x00, (byte) 0x10,
        (byte) 0x7F, (byte) 0x00, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x20,
    };
    private AttributeRegistry handler;

    @Before
    public void setUp() {
        final BGPExtensionProviderContext ctx = new SimpleBGPExtensionProviderContext();

        final org.opendaylight.protocol.bgp.parser.impl.BGPActivator inetActivator =
                new org.opendaylight.protocol.bgp.parser.impl.BGPActivator();
        inetActivator.start(ctx);
        final BGPActivator bgpActivator = new BGPActivator();
        bgpActivator.start(ctx);
        this.handler = ctx.getAttributeRegistry();
    }

    @Test
    public void testPEDistinguisherLabelsHandler() throws Exception {
        final Attributes expected = buildPEDistinguisherLabelsAttributAttribute();
        final ByteBuf actual = Unpooled.buffer();
        this.handler.serializeAttribute(expected, actual);
        assertArrayEquals(PE_DISTINGUISHER_LABELS, ByteArray.readAllBytes(actual));
        final Attributes actualAttr = this.handler.parseAttributes(
                Unpooled.wrappedBuffer(PE_DISTINGUISHER_LABELS), null);
        assertEquals(expected, actualAttr);
    }

    private Attributes buildPEDistinguisherLabelsAttributAttribute() {
        final List<PeDistinguisherLabelAttribute> peAtt = new ArrayList<>();

        peAtt.add(new PeDistinguisherLabelAttributeBuilder()
                .setPeAddress(new IpAddress(new Ipv4Address("127.0.0.1")))
                .setMplsLabel(new MplsLabel(1L))
                .build());
        peAtt.add(new PeDistinguisherLabelAttributeBuilder()
                .setPeAddress(new IpAddress(new Ipv4Address("127.0.0.2")))
                .setMplsLabel(new MplsLabel(2L))
                .build());
        return new AttributesBuilder()
                .setUnrecognizedAttributes(Collections.emptyList())
                .addAugmentation(PeDistinguisherLabelsAttributeAugmentation.class,
                        new PeDistinguisherLabelsAttributeAugmentationBuilder()
                                .setPeDistinguisherLabelsAttribute(new PeDistinguisherLabelsAttributeBuilder()
                                        .setPeDistinguisherLabelAttribute(peAtt).build()).build()).build();
    }
}