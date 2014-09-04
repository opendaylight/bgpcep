/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.crabbe.initiated00.InitiatedActivator;
import org.opendaylight.protocol.pcep.crabbe.initiated00.PcinitiateMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02PCReplyMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02PCReportMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02PCRequestMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02PCUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.Activator;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.P2p1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.P2p1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Replies1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Replies1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

public class PCEPMessageTest {

    private Lspa lspa;
    private Metrics metrics;
    private Ero ero;
    private EndpointsObj endpoints;
    private AsNumberCase eroASSubobject;
    private Activator act;
    private Lsp lsp;
    private Rp rpTrue;
    private Iro iro;
    private Bandwidth bandwidth;

    private SimplePCEPExtensionProviderContext ctx;

    @Before
    public void setUp() throws Exception {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new Activator();
        this.act.start(this.ctx);

        final LspaBuilder lspaBuilder = new LspaBuilder();
        lspaBuilder.setProcessingRule(false);
        lspaBuilder.setIgnore(false);
        lspaBuilder.setLocalProtectionDesired(false);
        lspaBuilder.setHoldPriority((short) 0);
        lspaBuilder.setSetupPriority((short) 0);
        lspaBuilder.setExcludeAny(new AttributeFilter(0L));
        lspaBuilder.setIncludeAll(new AttributeFilter(0L));
        lspaBuilder.setIncludeAny(new AttributeFilter(0L));
        lspaBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder().build());
        this.lspa = lspaBuilder.build();

        final LspBuilder lspBuilder = new LspBuilder();
        lspBuilder.setIgnore(false);
        lspBuilder.setProcessingRule(false);
        lspBuilder.setDelegate(false);
        lspBuilder.setPlspId(new PlspId(0L));
        lspBuilder.setOperational(false);
        lspBuilder.setSync(false);
        lspBuilder.setRemove(false);
        lspBuilder.setTlvs(new TlvsBuilder().build());
        this.lsp = lspBuilder.build();

        final MetricBuilder mBuilder = new MetricBuilder();
        mBuilder.setIgnore(false);
        mBuilder.setProcessingRule(false);
        mBuilder.setComputed(false);
        mBuilder.setBound(false);
        mBuilder.setMetricType((short) 1);
        mBuilder.setValue(new Float32(new byte[4]));
        this.metrics = new MetricsBuilder().setMetric(mBuilder.build()).build();

        this.eroASSubobject = new AsNumberCaseBuilder().setAsNumber(
                new AsNumberBuilder().setAsNumber(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber(0xFFFFL)).build()).build();

        final EroBuilder eroBuilder = new EroBuilder();
        eroBuilder.setIgnore(false);
        eroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject> eroSubs = Lists.newArrayList();
        eroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setSubobjectType(
                this.eroASSubobject).setLoose(true).build());
        eroBuilder.setSubobject(eroSubs);
        this.ero = eroBuilder.build();

        final Ipv4Builder afi = new Ipv4Builder();
        afi.setSourceIpv4Address(new Ipv4Address("162.245.17.14"));
        afi.setDestinationIpv4Address(new Ipv4Address("255.255.255.255"));

        final EndpointsObjBuilder epBuilder = new EndpointsObjBuilder();
        epBuilder.setIgnore(false);
        epBuilder.setProcessingRule(true);
        epBuilder.setAddressFamily(new Ipv4CaseBuilder().setIpv4(afi.build()).build());
        this.endpoints = epBuilder.build();

        final RpBuilder rpBuilder = new RpBuilder();
        rpBuilder.setProcessingRule(true);
        rpBuilder.setIgnore(false);
        rpBuilder.setReoptimization(false);
        rpBuilder.setBiDirectional(false);
        rpBuilder.setLoose(true);
        rpBuilder.setMakeBeforeBreak(false);
        rpBuilder.setOrder(false);
        rpBuilder.setPathKey(false);
        rpBuilder.setSupplyOf(false);
        rpBuilder.setFragmentation(false);
        rpBuilder.setP2mp(false);
        rpBuilder.setEroCompression(false);
        rpBuilder.setPriority((short) 1);
        rpBuilder.setRequestId(new RequestId(10L));
        rpBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder().build());
        this.rpTrue = rpBuilder.build();

        final IroBuilder iroBuilder = new IroBuilder();
        iroBuilder.setIgnore(false);
        iroBuilder.setProcessingRule(false);
        final List<Subobject> iroSubs = Lists.newArrayList();
        iroSubs.add(new SubobjectBuilder().setSubobjectType(this.eroASSubobject).build());
        iroBuilder.setSubobject(iroSubs);
        this.iro = iroBuilder.build();

        this.bandwidth = new BandwidthBuilder().setIgnore(false).setProcessingRule(false).setBandwidth(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(new byte[] {0x00, 0x00, 0x00, 0x00})).build();
    }

    @Test
    public void testPcinitMsg() throws IOException, PCEPDeserializerException {
        try (InitiatedActivator a = new InitiatedActivator()) {
            a.start(this.ctx);
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/Pcinit.bin"));

            final PcinitiateMessageParser parser = new PcinitiateMessageParser(this.ctx.getObjectHandlerRegistry());

            final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
            final RequestsBuilder rBuilder = new RequestsBuilder();

            final List<Requests> reqs = Lists.newArrayList();
            rBuilder.setEndpointsObj(this.endpoints);
            rBuilder.setLspa(this.lspa);
            rBuilder.setEro(this.ero);
            rBuilder.setMetrics(Lists.newArrayList(this.metrics));
            reqs.add(rBuilder.build());
            builder.setRequests(reqs);

            assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            final ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testUpdMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin"));

            final Stateful02PCUpdateRequestMessageParser parser = new Stateful02PCUpdateRequestMessageParser(this.ctx.getObjectHandlerRegistry());

            final PcupdMessageBuilder builder = new PcupdMessageBuilder();

            final List<Updates> updates = Lists.newArrayList();
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            pBuilder.setIro(this.iro);
            pBuilder.setMetrics(Lists.newArrayList(this.metrics));
            pBuilder.setBandwidth(this.bandwidth);
            updates.add(new UpdatesBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
            builder.setUpdates(updates);

            assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.5.bin"));

            final List<Updates> updates1 = Lists.newArrayList();
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder();
            pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            pBuilder1.setEro(this.ero);
            pBuilder1.setLspa(this.lspa);
            updates1.add(new UpdatesBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
            updates1.add(new UpdatesBuilder().setLsp(this.lsp).setPath(pBuilder1.build()).build());
            builder.setUpdates(updates1);

            assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testRptMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.1.bin"));

            final Stateful02PCReportMessageParser parser = new Stateful02PCReportMessageParser(this.ctx.getObjectHandlerRegistry());

            final PcrptMessageBuilder builder = new PcrptMessageBuilder();

            final List<Reports> reports = Lists.newArrayList();
            reports.add(new ReportsBuilder().setLsp(this.lsp).build());
            builder.setReports(reports);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.2.bin"));

            final List<Reports> reports1 = Lists.newArrayList();
            reports1.add(new ReportsBuilder().setLsp(this.lsp).setPath(new PathBuilder().setEro(this.ero).setLspa(this.lspa).build()).build());
            builder.setReports(reports1);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin"));

            final List<Reports> reports2 = Lists.newArrayList();
            final PathBuilder pBuilder = new PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            pBuilder.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
            reports2.add(new ReportsBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
            builder.setReports(reports2);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.5.bin"));

            final List<Reports> reports3 = Lists.newArrayList();
            final PathBuilder pBuilder1 = new PathBuilder();
            pBuilder1.setEro(this.ero);
            pBuilder1.setLspa(this.lspa);
            pBuilder1.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
            reports3.add(new ReportsBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
            reports3.add(new ReportsBuilder().setLsp(this.lsp).setPath(pBuilder1.build()).build());
            builder.setReports(reports3);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                    result.readableBytes() - 4), Collections.<Message> emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testPcReqMsg() throws PCEPDeserializerException, IOException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);

            final OfBuilder ofBuilder = new OfBuilder();
            ofBuilder.setIgnore(false);
            ofBuilder.setProcessingRule(false);
            ofBuilder.setCode(new OfId(0));
            ofBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.of.TlvsBuilder().build());

            final RroBuilder rroBuilder = new RroBuilder();
            rroBuilder.setSubobject(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setProtectionAvailable(false).setProtectionInUse(false).setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("127.1.0.0/32"))).build()).build()).build()));
            rroBuilder.setIgnore(false);
            rroBuilder.setProcessingRule(false);

            final Stateful02PCRequestMessageParser parser = new Stateful02PCRequestMessageParser(this.ctx.getObjectHandlerRegistry(), this.ctx.getVendorInformationObjectRegistry());
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPRequestMessage1.bin"));
            final PcreqMessageBuilder builder = new PcreqMessageBuilder();
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests> reqs1 = Lists.newArrayList();
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder rBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder();
            rBuilder.setRp(this.rpTrue);
            rBuilder.setSegmentComputation(new SegmentComputationBuilder().setP2p(new P2pBuilder()
                .addAugmentation(P2p1.class, new P2p1Builder().setLsp(this.lsp).build())
                .setEndpointsObj(this.endpoints)
                .setBandwidth(this.bandwidth)
                .setIro(this.iro)
                .setLoadBalancing(new LoadBalancingBuilder().setMaxLsp((short) 1).setMinBandwidth(this.bandwidth.getBandwidth()).setIgnore(false).setProcessingRule(false).build())
                .setLspa(this.lspa)
                .setMetrics(Lists.newArrayList(this.metrics))
                .setOf(ofBuilder.build())
                .setRro(rroBuilder.build())
                .setClassType(new ClassTypeBuilder().setProcessingRule(true).setIgnore(false).setClassType(new ClassType((short) 1)).build())
                .build()).build());
            reqs1.add(rBuilder.build());
            builder.setRequests(reqs1);

            parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.<Message> emptyList());
            assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.<Message> emptyList()));
            final ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testPcRepMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            final Stateful02PCReplyMessageParser parser = new Stateful02PCReplyMessageParser(this.ctx.getObjectHandlerRegistry(), this.ctx.getVendorInformationObjectRegistry());

            final OfBuilder ofBuilder = new OfBuilder();
            ofBuilder.setIgnore(false);
            ofBuilder.setProcessingRule(false);
            ofBuilder.setCode(new OfId(0));
            ofBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.of.TlvsBuilder().build());

            // test success case
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.5.bin"));
            final PcrepMessageBuilder builder = new PcrepMessageBuilder();
            final List<Replies> replies4 = Lists.newArrayList();
            RepliesBuilder rBuilder = new RepliesBuilder();
            rBuilder.setRp(this.rpTrue);
            rBuilder.addAugmentation(Replies1.class, new Replies1Builder().setLsp(this.lsp).build());
            final List<Paths> paths = Lists.newArrayList();
            final PathsBuilder paBuilder = new PathsBuilder();
            paBuilder.setEro(this.ero);
            paBuilder.setLspa(this.lspa);
            paBuilder.setMetrics(Lists.newArrayList(this.metrics));
            paBuilder.setIro(this.iro);
            paBuilder.setOf(ofBuilder.build());
            paths.add(paBuilder.build());
            rBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).build()).build()).build();
            replies4.add(rBuilder.build());
            builder.setReplies(replies4);

            assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.<Message> emptyList()));
            final ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            // test failure case
            final NoPathBuilder npbuilder = new NoPathBuilder();
            npbuilder.setProcessingRule(false);
            npbuilder.setIgnore(false);
            npbuilder.setNatureOfIssue((short) 0);
            npbuilder.setUnsatisfiedConstraints(false);
            npbuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder().build());

            final ByteBuf result2 = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.3.bin"));
            final List<Replies> replies3 = Lists.newArrayList();
            rBuilder = new RepliesBuilder();
            rBuilder.setRp(this.rpTrue);
            rBuilder.addAugmentation(Replies1.class, new Replies1Builder().build());
            rBuilder.setResult(new FailureCaseBuilder().setNoPath(npbuilder.build()).setLspa(this.lspa).setMetrics(Lists.newArrayList(this.metrics)).setIro(
                this.iro).build());
            replies3.add(rBuilder.build());
            builder.setReplies(replies3);

            assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result2.slice(4,
                    result2.readableBytes() - 4), Collections.<Message> emptyList()));
            final ByteBuf buffer = Unpooled.buffer(result2.readableBytes());
            parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buffer);
            assertArrayEquals(result2.array(), buffer.array());
        }
    }
}
