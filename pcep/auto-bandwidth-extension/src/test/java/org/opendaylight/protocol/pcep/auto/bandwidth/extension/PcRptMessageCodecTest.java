/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import javax.xml.bind.DatatypeConverter;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulActivator;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.Bandwidth1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.Bandwidth1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev181109.bandwidth.usage.object.BandwidthUsageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class PcRptMessageCodecTest {
    private static final List<Bandwidth> BW = List.of(new Bandwidth(new byte[]{0, 0, 0, 1}));

    private final SimplePCEPExtensionProviderContext ctx = new SimplePCEPExtensionProviderContext();

    @Before
    public void setUp() {
        new BaseParserExtensionActivator().start(ctx);
        new StatefulActivator().start(ctx);
        new org.opendaylight.protocol.pcep.auto.bandwidth.extension.AutoBandwidthActivator().start(ctx);
    }

    @Test
    public void testGetValidReportsPositive() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(ctx.getObjectHandlerRegistry());
        final BandwidthUsage bw = new BandwidthUsageBuilder().setBwSample(BW).build();
        final Ipv4Builder builder = new Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(new Ipv4AddressNoZone("127.0.1.1"));
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4AddressNoZone("127.0.1.2")));
        builder.setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone("127.0.1.3"));
        final AddressFamily afiLsp = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        final LspId lspId = new LspId(Uint32.ONE);
        final TunnelId tunnelId = new TunnelId(Uint16.ONE);
        final LspIdentifiers identifier = new LspIdentifiersBuilder().setAddressFamily(afiLsp)
                .setLspId(lspId).setTunnelId(tunnelId).build();
        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(Uint32.ONE))
                .setTlvs(new TlvsBuilder().setLspIdentifiers(identifier).build()).build();
        final Ero ero = new EroBuilder().build();
        final Queue<Object> objects = new ArrayDeque<>(List.of(lsp, ero, bw));
        final Reports validReports = codec.getValidReports(objects, List.of());
        assertNotNull(validReports.getPath().getBandwidth().augmentation(Bandwidth1.class));
        assertTrue(objects.isEmpty());
    }

    @Test
    public void testGetValidReportsNegative() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(ctx.getObjectHandlerRegistry());
        final BandwidthUsage bw = new BandwidthUsageBuilder().setBwSample(BW).build();
        final Ipv4Builder builder = new Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(new Ipv4AddressNoZone("127.0.1.1"));
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4AddressNoZone("127.0.1.2")));
        builder.setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone("127.0.1.3"));
        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(Uint32.ONE)).build();
        final Ero ero = new EroBuilder().build();
        final Queue<Object> objects = new ArrayDeque<>(List.of(lsp, ero, bw));
        final Reports validReports = codec.getValidReports(objects, new ArrayList<>());
        assertNull(validReports);
    }

    @Test
    public void testserializeObject() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(ctx.getObjectHandlerRegistry());
        final ByteBuf buffer = Unpooled.buffer();
        codec.serializeObject(new BandwidthBuilder()
            .addAugmentation(new Bandwidth1Builder().setBwSample(BW).build())
            .build(), buffer);
        assertTrue(buffer.readableBytes() > 0);
    }

    @Test
    public void testReportMsgWithRro() throws PCEPDeserializerException {
        final byte[] parseHexBinary = DatatypeConverter
                .parseHexBinary("2010003c0084a019001100106e79636e7932316372735f7432313231001200100a0"
                        + "000d2004008490a0000d40a0000d4001f0006000005dd700000000710001401080a000706200001080a0000d420"
                        + "000910001400000000000000000000000005050100051000084998968005500008513a43b70810002401080a000"
                        + "0d42020030801010000000001080a00070620000308010100000000");
        final Pcrpt msg = (Pcrpt) ctx.getMessageHandlerRegistry().parseMessage(10,
                Unpooled.wrappedBuffer(parseHexBinary), Collections.emptyList());
        assertNotNull(msg.getPcrptMessage().getReports().get(0).getPath()
                .getBandwidth().augmentation(Bandwidth1.class));
    }

}
