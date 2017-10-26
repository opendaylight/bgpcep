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
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.Bandwidth1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.Bandwidth1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public class PcRptMessageCodecTest {

    private SimplePCEPExtensionProviderContext ctx;
    private BaseParserExtensionActivator act;
    private StatefulActivator statefulAct;
    private org.opendaylight.protocol.pcep.auto.bandwidth.extension.Activator autoBwActivator;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new BaseParserExtensionActivator();
        this.act.start(this.ctx);
        this.statefulAct = new StatefulActivator();
        this.statefulAct.start(this.ctx);
        this.autoBwActivator = new org.opendaylight.protocol.pcep.auto.bandwidth.extension.Activator(5);
        this.autoBwActivator.start(this.ctx);
    }

    @After
    public void tearDown() {
        this.act.stop();
        this.statefulAct.stop();
        this.autoBwActivator.stop();
    }

    @Test
    public void testGetValidReportsPositive() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(this.ctx.getObjectHandlerRegistry());
        final BandwidthUsage bw = new BandwidthUsageBuilder().setBwSample(Lists.newArrayList(new Bandwidth(new byte[] {0, 0, 0, 1}))).build();
        final Ipv4Builder builder = new Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(new Ipv4Address("127.0.1.1"));
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4Address("127.0.1.2")));
        builder.setIpv4TunnelEndpointAddress(new Ipv4Address("127.0.1.3"));
        final AddressFamily afiLsp = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        final LspId lspId = new LspId(1L);
        final TunnelId tunnelId = new TunnelId(1);
        final LspIdentifiers identifier = new LspIdentifiersBuilder().setAddressFamily(afiLsp).setLspId(lspId).setTunnelId(tunnelId).build();
        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(1L)).setTlvs(new TlvsBuilder().setLspIdentifiers(identifier).build()).build();
        final Ero ero = new EroBuilder().build();
        final List<Object> objects = Lists.newArrayList(lsp, ero, bw);
        final Reports validReports = codec.getValidReports(objects, Collections.emptyList());
        assertNotNull(validReports.getPath().getBandwidth().getAugmentation(Bandwidth1.class));
        assertTrue(objects.isEmpty());
    }

    @Test
    public void testGetValidReportsNegative() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(this.ctx.getObjectHandlerRegistry());
        final BandwidthUsage bw = new BandwidthUsageBuilder().setBwSample(Lists.newArrayList(new Bandwidth(new byte[] {0, 0, 0, 1}))).build();
        final Ipv4Builder builder = new Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(new Ipv4Address("127.0.1.1"));
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4Address("127.0.1.2")));
        builder.setIpv4TunnelEndpointAddress(new Ipv4Address("127.0.1.3"));
        final Lsp lsp = new LspBuilder().setPlspId(new PlspId(1L)).build();
        final Ero ero = new EroBuilder().build();
        final List<Object> objects = Lists.newArrayList(lsp, ero, bw);
        final Reports validReports = codec.getValidReports(objects, new ArrayList<>());
        assertNull(validReports);
    }

    @Test
    public void testserializeObject() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(this.ctx.getObjectHandlerRegistry());
        final BandwidthBuilder bwBuilder = new BandwidthBuilder();
        bwBuilder.addAugmentation(Bandwidth1.class, new Bandwidth1Builder().setBwSample(
                Lists.newArrayList(new Bandwidth(new byte[] {0, 0, 0, 1}))).build());
        final ByteBuf buffer = Unpooled.buffer();
        codec.serializeObject(bwBuilder.build(), buffer);
        Assert.assertTrue(buffer.readableBytes() > 0);
    }

    @Test
    public void testReportMsgWithRro() throws PCEPDeserializerException {
        final byte[] parseHexBinary = DatatypeConverter.parseHexBinary("2010003c0084a019001100106e79636e7932316372735f7432313231001200100a0000d2004008490a0000d40a0000d4001f0006000005dd700000000710001401080a000706200001080a0000d420000910001400000000000000000000000005050100051000084998968005500008513a43b70810002401080a0000d42020030801010000000001080a00070620000308010100000000");
        final Pcrpt msg = (Pcrpt) this.ctx.getMessageHandlerRegistry().parseMessage(10, Unpooled.wrappedBuffer(parseHexBinary), Collections.emptyList());
        Assert.assertNotNull(msg.getPcrptMessage().getReports().get(0).getPath().getBandwidth().getAugmentation(Bandwidth1.class));
    }

}
