/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.impl.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.c.close.TlvsBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.PcntfMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;

public class PCEPValidatorTest {

    private ObjectRegistry objectRegistry;
    private VendorInformationObjectRegistry viObjRegistry;

    private Open open;
    private Rp rpTrue;
    private Rp rpFalse;
    private NoPath noPath;
    private Lspa lspa;
    private Metrics metrics;
    private Iro iro;
    private Ero ero;
    private Of of;
    private EndpointsObj endpoints;
    private Svec svec;
    private List<VendorInformationObject> viObjects;

    private AsNumberCase eroASSubobject;

    private SimplePCEPExtensionProviderContext ctx;
    private Activator act;
    private TestVendorInformationActivator viObjAct;

    @Before
    public void setUp() throws Exception {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new Activator();
        this.viObjAct = new TestVendorInformationActivator();
        this.act.start(this.ctx);
        this.viObjAct.start(this.ctx);
        this.objectRegistry = this.ctx.getObjectHandlerRegistry();
        this.viObjRegistry = this.ctx.getVendorInformationObjectRegistry();
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
        rpBuilder.setProcessingRule(false);
        this.rpFalse = rpBuilder.build();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder openBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder();
        openBuilder.setProcessingRule(false);
        openBuilder.setIgnore(false);
        openBuilder.setDeadTimer((short) 1);
        openBuilder.setKeepalive((short) 1);
        openBuilder.setSessionId((short) 0);
        openBuilder.setVersion(new ProtocolVersion((short) 1));
        openBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().build());
        this.open = openBuilder.build();

        final NoPathBuilder npbuilder = new NoPathBuilder();
        npbuilder.setProcessingRule(false);
        npbuilder.setIgnore(false);
        npbuilder.setNatureOfIssue((short) 0);
        npbuilder.setUnsatisfiedConstraints(false);
        npbuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder().build());
        this.noPath = npbuilder.build();

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

        final IroBuilder iroBuilder = new IroBuilder();
        iroBuilder.setIgnore(false);
        iroBuilder.setProcessingRule(false);
        final List<Subobject> iroSubs = Lists.newArrayList();
        iroSubs.add(new SubobjectBuilder().setSubobjectType(this.eroASSubobject).build());
        iroBuilder.setSubobject(iroSubs);
        this.iro = iroBuilder.build();

        final EroBuilder eroBuilder = new EroBuilder();
        eroBuilder.setIgnore(false);
        eroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject> eroSubs = Lists.newArrayList();
        eroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setSubobjectType(
            this.eroASSubobject).setLoose(false).build());
        eroBuilder.setSubobject(eroSubs);
        this.ero = eroBuilder.build();

        final OfBuilder ofBuilder = new OfBuilder();
        ofBuilder.setIgnore(false);
        ofBuilder.setProcessingRule(false);
        ofBuilder.setCode(new OfId(0));
        ofBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.of.TlvsBuilder().build());
        this.of = ofBuilder.build();

        final Ipv4Builder afi = new Ipv4Builder();
        afi.setSourceIpv4Address(new Ipv4Address("255.255.255.255"));
        afi.setDestinationIpv4Address(new Ipv4Address("255.255.255.255"));

        final EndpointsObjBuilder epBuilder = new EndpointsObjBuilder();
        epBuilder.setIgnore(false);
        epBuilder.setProcessingRule(true);
        epBuilder.setAddressFamily(new Ipv4CaseBuilder().setIpv4(afi.build()).build());
        this.endpoints = epBuilder.build();

        final SvecBuilder sBuilder = new SvecBuilder();
        sBuilder.setIgnore(false);
        sBuilder.setProcessingRule(false);
        sBuilder.setLinkDiverse(false);
        sBuilder.setNodeDiverse(false);
        sBuilder.setSrlgDiverse(false);
        sBuilder.setRequestsIds(Lists.newArrayList(new RequestId(1L)));
        this.svec = sBuilder.build();

        this.viObjects = Lists.newArrayList();
        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationObject viObj = new VendorInformationObjectBuilder().setEnterpriseNumber(new EnterpriseNumber(0L))
                .setEnterpriseSpecificInformation(esInfo).build();
        this.viObjects.add(viObj);
    }

    @Test
    public void testOpenMsg() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPOpenMessage1.bin"));
        final PCEPOpenMessageParser parser = new PCEPOpenMessageParser(this.ctx.getObjectHandlerRegistry());
        final OpenMessageBuilder builder = new OpenMessageBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder();
        b.setProcessingRule(false);
        b.setIgnore(false);
        b.setVersion(new ProtocolVersion((short) 1));
        b.setKeepalive((short) 30);
        b.setDeadTimer((short) 120);
        b.setSessionId((short) 1);
        b.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().build());
        builder.setOpen(b.build());

        assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new OpenBuilder().setOpenMessage(new OpenMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Open Object must be present in Open Message.", e.getMessage());
        }
    }

    @Test
    public void testKeepAliveMsg() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] { 32, 2, 0, 4 });
        final PCEPKeepAliveMessageParser parser = new PCEPKeepAliveMessageParser(this.objectRegistry);
        final KeepaliveBuilder builder = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build());

        assertEquals(builder.build(), parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.<Message> emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(builder.build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testCloseMsg() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPCloseMessage1.bin"));

        final PCEPCloseMessageParser parser = new PCEPCloseMessageParser(this.objectRegistry);
        final CloseBuilder builder = new CloseBuilder().setCCloseMessage(new CCloseMessageBuilder().setCClose(
            new CCloseBuilder().setIgnore(false).setProcessingRule(false).setReason((short) 5).setTlvs(new TlvsBuilder().build()).build()).build());

        assertEquals(builder.build(), parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.<Message> emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(builder.build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new CloseBuilder().setCCloseMessage(new CCloseMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Close Object must be present in Close Message.", e.getMessage());
        }
    }

    @Test
    public void testRequestMsg() throws IOException, PCEPDeserializerException {
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPRequestMessage1.bin"));

        final PCEPRequestMessageParser parser = new PCEPRequestMessageParser(this.objectRegistry, this.viObjRegistry);

        final PcreqMessageBuilder builder = new PcreqMessageBuilder();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests> reqs1 = Lists.newArrayList();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder rBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder();
        rBuilder.setRp(this.rpTrue);
        rBuilder.setSegmentComputation(new SegmentComputationBuilder().setP2p(new P2pBuilder().setEndpointsObj(this.endpoints).build()).build());
        reqs1.add(rBuilder.build());
        builder.setRequests(reqs1);

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCReq.3.bin"));

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests> reqs2 = Lists.newArrayList();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder rBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder();
        rBuilder1.setRp(this.rpTrue);
        final P2pBuilder p2pBuilder = new P2pBuilder();
        p2pBuilder.setEndpointsObj(this.endpoints);
        p2pBuilder.setMetrics(Lists.newArrayList(this.metrics));
        p2pBuilder.setIro(this.iro);
        rBuilder1.setSegmentComputation(new SegmentComputationBuilder().setP2p(p2pBuilder.build()).build());
        reqs2.add(rBuilder1.build());
        builder.setRequests(reqs2);
        builder.setSvec(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder().setSvec(
            this.svec).build()));

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new PcreqBuilder().setPcreqMessage(new PcreqMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Requests cannot be null or empty.", e.getMessage());
        }
        try {
            parser.serializeMessage(new PcreqBuilder().setPcreqMessage(new PcreqMessageBuilder().setRequests(Collections.<Requests> emptyList()).build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Requests cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testReplyMsg() throws IOException, PCEPDeserializerException {
        // only RP
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.1.bin"));

        final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(this.objectRegistry, this.viObjRegistry);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();
        RepliesBuilder rBuilder = new RepliesBuilder();

        final List<Replies> replies1 = Lists.newArrayList();
        rBuilder.setRp(this.rpTrue);
        replies1.add(rBuilder.build());
        builder.setReplies(replies1);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        // simple Failure
        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.2.bin"));
        final List<Replies> replies2 = Lists.newArrayList();
        rBuilder = new RepliesBuilder();
        rBuilder.setRp(this.rpTrue);
        replies2.add(rBuilder.build());
        final RepliesBuilder rBuilder2 = new RepliesBuilder();
        rBuilder2.setRp(this.rpTrue);
        rBuilder2.setResult(new FailureCaseBuilder().setNoPath(this.noPath).build());
        replies2.add(rBuilder2.build());
        builder.setReplies(replies2);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        // Failure with attributes
        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.3.bin"));
        final List<Replies> replies3 = Lists.newArrayList();
        rBuilder = new RepliesBuilder();
        rBuilder.setRp(this.rpTrue);
        rBuilder.setResult(new FailureCaseBuilder().setNoPath(this.noPath).setLspa(this.lspa).setMetrics(Lists.newArrayList(this.metrics)).setIro(
            this.iro).build());
        replies3.add(rBuilder.build());
        builder.setReplies(replies3);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        // Success
        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.5.bin"));
        final List<Replies> replies4 = Lists.newArrayList();
        rBuilder = new RepliesBuilder();
        rBuilder.setRp(this.rpTrue);
        final List<Paths> paths = Lists.newArrayList();
        final PathsBuilder paBuilder = new PathsBuilder();
        paBuilder.setEro(this.ero);
        paBuilder.setLspa(this.lspa);
        paBuilder.setMetrics(Lists.newArrayList(this.metrics));
        paBuilder.setIro(this.iro);
        paBuilder.setOf(this.of);
        paths.add(paBuilder.build());
        rBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).build()).build()).build();
        replies4.add(rBuilder.build());
        builder.setReplies(replies4);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Replies cannot be null or empty.", e.getMessage());
        }
        try {
            parser.serializeMessage(new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder().setReplies(Collections.<Replies> emptyList()).build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Replies cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testNotificationMsg() throws IOException, PCEPDeserializerException {
        final CNotification cn1 = new CNotificationBuilder().setIgnore(false).setProcessingRule(false).setType((short) 1).setValue(
            (short) 1).build();

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications> innerNot = Lists.newArrayList();
        innerNot.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder().setCNotification(
            cn1).build());
        final List<Rps> rps = Lists.newArrayList();
        rps.add(new RpsBuilder().setRp(this.rpFalse).build());

        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCNtf.5.bin"));

        final PCEPNotificationMessageParser parser = new PCEPNotificationMessageParser(this.objectRegistry);
        final PcntfMessageBuilder builder = new PcntfMessageBuilder();

        final List<Notifications> nots = Lists.newArrayList();
        final NotificationsBuilder b = new NotificationsBuilder();
        b.setNotifications(innerNot);
        b.setRps(rps);
        nots.add(b.build());

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications> innerNot1 = Lists.newArrayList();
        innerNot1.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder().setCNotification(
            cn1).build());
        innerNot1.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder().setCNotification(
            cn1).build());
        final List<Rps> rps1 = Lists.newArrayList();
        rps1.add(new RpsBuilder().setRp(this.rpFalse).build());
        rps1.add(new RpsBuilder().setRp(this.rpFalse).build());

        b.setNotifications(innerNot1);
        b.setRps(rps1);
        nots.add(b.build());
        builder.setNotifications(nots);

        assertEquals(new PcntfBuilder().setPcntfMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcntfBuilder().setPcntfMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testErrorMsg() throws IOException, PCEPDeserializerException {
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.5.bin"));

        ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

        final PCEPErrorMessageParser parser = new PCEPErrorMessageParser(this.ctx.getObjectHandlerRegistry());

        List<Errors> innerErr = new ArrayList<>();

        final PcerrMessageBuilder builder = new PcerrMessageBuilder();

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.Rps> rps = Lists.newArrayList();
        rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder().setRp(
            this.rpFalse).build());

        innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

        builder.setErrors(innerErr);
        builder.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(rps).build()).build());

        assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.3.bin"));

        error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

        innerErr = new ArrayList<>();
        innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

        builder.setErrors(innerErr);
        builder.setErrorType(new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(this.open).build()).build());

        assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(new PcerrMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Errors should not be empty.", e.getMessage());
        }
        try {
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(new PcerrMessageBuilder().setErrors(Collections.<Errors> emptyList()).build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Errors should not be empty.", e.getMessage());
        }
    }

    @Test
    public void testReqMsgWithVendorInfoObjects() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCReq.7.bin"));
        final PCEPRequestMessageParser parser = new PCEPRequestMessageParser(this.objectRegistry, this.viObjRegistry);

        final PcreqMessageBuilder builder = new PcreqMessageBuilder();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests> reqs1 = Lists.newArrayList();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder rBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder();
        rBuilder.setRp(this.rpTrue);
        rBuilder.setVendorInformationObject(this.viObjects);
        final SegmentComputationBuilder sBuilder = new SegmentComputationBuilder();
        sBuilder.setP2p(new P2pBuilder().setEndpointsObj(this.endpoints).setVendorInformationObject(this.viObjects).build());
        rBuilder.setSegmentComputation(sBuilder.build());
        reqs1.add(rBuilder.build());
        builder.setSvec(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder().setSvec(
                this.svec).setVendorInformationObject(this.viObjects).build()));
        builder.setRequests(reqs1);

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testRepMsgWithVendorInforObjects() throws IOException, PCEPDeserializerException {
        final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(this.objectRegistry, this.viObjRegistry);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();
        RepliesBuilder rBuilder = new RepliesBuilder();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.6.bin"));
        final List<Replies> replies = Lists.newArrayList();
        rBuilder = new RepliesBuilder();
        rBuilder.setRp(this.rpTrue);
        rBuilder.setVendorInformationObject(this.viObjects);
        final List<Paths> paths = Lists.newArrayList();
        final PathsBuilder paBuilder = new PathsBuilder();
        paBuilder.setEro(this.ero);
        paths.add(paBuilder.build());
        rBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).setVendorInformationObject(this.viObjects).build()).build()).build();
        replies.add(rBuilder.build());
        builder.setReplies(replies);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.<Message> emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }
}
