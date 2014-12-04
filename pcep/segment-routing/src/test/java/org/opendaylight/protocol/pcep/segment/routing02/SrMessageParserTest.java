/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.Activator;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.SrEroSubobject.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.ero.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;

public class SrMessageParserTest {

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator srActivator;
    private Activator activator;
    private StatefulActivator statefulActivator;
    private ObjectRegistry objectRegistry;
    private VendorInformationObjectRegistry viObjReg;

    @Before
    public void setup() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.activator = new Activator();
        this.activator.start(this.ctx);
        this.statefulActivator = new StatefulActivator();
        this.statefulActivator.start(this.ctx);
        this.srActivator = new SegmentRoutingActivator();
        this.srActivator.start(this.ctx);
        this.objectRegistry = this.ctx.getObjectHandlerRegistry();
        this.viObjReg = this.ctx.getVendorInformationObjectRegistry();
    }

    @Test
    public void testReplyMsg() throws IOException, PCEPDeserializerException {
        byte[] replyMsgBytes = {
            0x20,0x04,0x00,0x28,
            /* rp-object */
            0x02,0x10,0x00,0x14,
            0x00,0x00,0x04,0x2d,
            (byte)0xde,(byte)0xad,(byte)0xbe,(byte)0xef,
            /* pst-tlv */
            0x00,0x1b,0x00,0x04,
            0x00,0x00,0x00,0x01,
            /* sr-ero-object */
            0x07,0x10,0x00,0x10,
            /* sr-ero subobject */
            0x05,0x0c,(byte) 0x10,0x00,
            0x00,0x01,(byte) 0xe2,0x40,
            0x4A,0x7D,0x2b,0x63};

        final SrPcRepMessageParser parser = new SrPcRepMessageParser(this.objectRegistry, this.viObjReg);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();
        final RepliesBuilder rBuilder = new RepliesBuilder();

        final List<Replies> replies = Lists.newArrayList();
        rBuilder.setRp(createRpObject());

        final List<Paths> paths = Lists.newArrayList();
        final PathsBuilder paBuilder = new PathsBuilder();

        paBuilder.setEro(createSrEroObject());

        paths.add(paBuilder.build());
        rBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).build()).build()).build();
        replies.add(rBuilder.build());
        builder.setReplies(replies);

        ByteBuf buf = Unpooled.wrappedBuffer(replyMsgBytes);
        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(buf.slice(4,
                buf.readableBytes() - 4), Collections.<Message> emptyList()));

        buf = Unpooled.buffer(replyMsgBytes.length);
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(replyMsgBytes, buf.array());
    }

    @Test
    public void testInitiateMsg() throws PCEPDeserializerException {

        final byte[] statefulMsg= {
            0x20,0x0C,0x00,0x30,
            /* srp-object */
            0x21,0x10,0x00,0x14,
            0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x01,
            /* pst-tlv */
            0x00,0x1b,0x00,0x04,
            0x00,0x00,0x00,0x01,
            /* lsp-object */
            0x20,0x10,0x00,0x08,
            0x00,0x00,0x00,0x00,
            /* sr-ero-object */
            0x07,0x10,0x00,0x10,
            /* sr-ero subobject */
            0x05,0x0c,(byte) 0x10,0x00,
            0x00,0x01,(byte) 0xe2,0x40,
            0x4A,0x7D,0x2b,0x63
        };

        final SrPcInitiateMessageParser parser = new SrPcInitiateMessageParser(objectRegistry);

        final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
        final RequestsBuilder reqBuilder = new RequestsBuilder();
        reqBuilder.setLsp(createLspObject());
        reqBuilder.setSrp(createSrpObject());

        reqBuilder.setEro(createSrEroObject());
        builder.setRequests(Lists.newArrayList(reqBuilder.build()));

        final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
        assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), parser.parseMessage(buf.slice(4,
                buf.readableBytes() - 4), Collections.<Message> emptyList()));

        final ByteBuf buffer = Unpooled.buffer(statefulMsg.length);
        parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buffer);
        assertArrayEquals(statefulMsg, buffer.array());
    }

    @Test
    public void testRptMsg() throws PCEPDeserializerException {
        final byte[] statefulMsg= {
            0x20,0x0A,0x00,0x30,
            /* srp-object */
            0x21,0x10,0x00,0x14,
            0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x01,
            /* pst-tlv */
            0x00,0x1b,0x00,0x04,
            0x00,0x00,0x00,0x01,
            /* lsp-object */
            0x20,0x10,0x00,0x08,
            0x00,0x00,0x00,0x00,
            /* sr-ero-object */
            0x07,0x10,0x00,0x10,
            /* sr-ero subobject */
            0x05,0x0c,(byte) 0x10,0x00,
            0x00,0x01,(byte) 0xe2,0x40,
            0x4A,0x7D,0x2b,0x63};

        final SrPcRptMessageParser parser = new SrPcRptMessageParser(objectRegistry);
        final PcrptMessageBuilder builder = new PcrptMessageBuilder();
        final ReportsBuilder rptBuilder = new ReportsBuilder();
        final Lsp lsp = createLspObject();
        final LspBuilder lspBuilder = new LspBuilder(lsp);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder tlvsBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder(lsp.getTlvs());
        tlvsBuilder.setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(0L)).build());
        lspBuilder.setTlvs(tlvsBuilder.build());
        rptBuilder.setLsp(lspBuilder.build());
        rptBuilder.setSrp(createSrpObject());
        rptBuilder.setPath(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder().setEro(createSrEroObject()).build());
        builder.setReports(Lists.newArrayList(rptBuilder.build()));

        final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
        assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(buf.slice(4,
                buf.readableBytes() - 4), Collections.<Message> emptyList()));

        final ByteBuf buffer = Unpooled.buffer(statefulMsg.length);
        rptBuilder.setLsp(createLspObject());
        builder.setReports(Lists.newArrayList(rptBuilder.build()));
        parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buffer);
        assertArrayEquals(statefulMsg, buffer.array());
    }

    @Test
    public void testUpdMsg() throws PCEPDeserializerException {
        final byte[] statefulMsg= {
            0x20,0x0B,0x00,0x30,
            /* srp-object */
            0x21,0x10,0x00,0x14,
            0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x01,
            /* pst-tlv */
            0x00,0x1b,0x00,0x04,
            0x00,0x00,0x00,0x01,
            /* lsp-object */
            0x20,0x10,0x00,0x08,
            0x00,0x00,0x00,0x00,
            /* sr-ero-object */
            0x07,0x10,0x00,0x10,
            /* sr-ero subobject */
            0x05,0x0c,(byte) 0x10,0x00,
            0x00,0x01,(byte) 0xe2,0x40,
            0x4A,0x7D,0x2b,0x63};

        final SrPcUpdMessageParser parser = new SrPcUpdMessageParser(objectRegistry);
        final PcupdMessageBuilder builder = new PcupdMessageBuilder();
        final UpdatesBuilder updBuilder = new UpdatesBuilder();
        updBuilder.setLsp(createLspObject());
        updBuilder.setSrp(createSrpObject());
        updBuilder.setPath(new PathBuilder().setEro(createSrEroObject()).build());
        builder.setUpdates(Lists.newArrayList(updBuilder.build()));

        final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
        assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(), parser.parseMessage(buf.slice(4,
                buf.readableBytes() - 4), Collections.<Message> emptyList()));

        final ByteBuf buffer = Unpooled.buffer(statefulMsg.length);
        parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buffer);
        assertArrayEquals(statefulMsg, buffer.array());
    }

    @Test
    public void testNonIdenticalEroSubobjectError() throws PCEPDeserializerException {
        final byte[] statefulMsg= {
            0x20,0x0B,0x00,0x30,
            /* srp-object */
            0x21,0x10,0x00,0x14,
            0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x01,
            /* pst-tlv */
            0x00,0x1b,0x00,0x04,
            0x00,0x00,0x00,0x01,
            /* lsp-object */
            0x20,0x10,0x00,0x08,
            0x00,0x00,0x00,0x00,
            /* sr-ero-object */
            0x07,0x10,0x00,0x18,
            /* sr-ero subobject */
            0x05,0x0c,(byte) 0x10,0x00,
            0x00,0x01,(byte) 0xe2,0x40,
            0x4A,0x7D,0x2b,0x63,
            /* ipv4 prefix subobject */
            (byte) 0x81,0x08,(byte) 0xFF,(byte) 0xFF,
            (byte) 0xFF,(byte) 0xFF,0x16,0x00};

        final SrPcRptMessageParser parser = new SrPcRptMessageParser(objectRegistry);

        final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
        List<Message> errors = Lists.newArrayList();
        parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);

        assertFalse(errors.isEmpty());
        assertEquals(createErrorMessage((short)10, (short)5), errors.get(0));
    }

    @Test
    public void testBadLabelValueError() throws PCEPDeserializerException {
        final byte[] statefulMsg= {
            0x20,0x0A,0x00,0x30,
            /* srp-object */
            0x21,0x10,0x00,0x14,
            0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x01,
            /* pst-tlv */
            0x00,0x1b,0x00,0x04,
            0x00,0x00,0x00,0x01,
            /* lsp-object */
            0x20,0x10,0x00,0x08,
            0x00,0x00,0x00,0x00,
            /* sr-ero-object */
            0x07,0x10,0x00,0x10,
            /* sr-ero subobject */
            0x05,0x0c,(byte) 0x10,0x01,
            0x00,0x00,0x00,0x03,
            0x4A,0x7D,0x2b,0x63};

        final SrPcRptMessageParser parser = new SrPcRptMessageParser(objectRegistry);

        final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
        final List<Message> errors = Lists.newArrayList();
        parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);

        assertFalse(errors.isEmpty());
        assertEquals(createErrorMessage((short)10, (short)2), errors.get(0));
    }

    private Ero createSrEroObject() {
        final SrEroTypeBuilder srEroBuilder = new SrEroTypeBuilder();
        srEroBuilder.setFlags(new Flags(false, false, false, false));
        srEroBuilder.setSidType(SidType.Ipv4NodeId);
        srEroBuilder.setSid(123456L);
        srEroBuilder.setNai(new IpNodeIdBuilder().setIpAddress(new IpAddress(new Ipv4Address("74.125.43.99"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(srEroBuilder.build()).setLoose(false);

        final List<Subobject> subobjects = Lists.newArrayList(subobjBuilder.build());
        return new EroBuilder().setProcessingRule(false).setIgnore(false).setSubobject(subobjects).build();
    }

    private Lsp createLspObject() {
        final LspBuilder lspBuilder = new LspBuilder();
        lspBuilder.setIgnore(false);
        lspBuilder.setProcessingRule(false);
        lspBuilder.setAdministrative(false);
        lspBuilder.setDelegate(false);
        lspBuilder.setPlspId(new PlspId(0L));
        lspBuilder.setOperational(OperationalStatus.Down);
        lspBuilder.setSync(false);
        lspBuilder.setRemove(false);
        lspBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().build());
        return lspBuilder.build();
    }

    private Rp createRpObject() {
        final RpBuilder rpBuilder = new RpBuilder();
        rpBuilder.setProcessingRule(false);
        rpBuilder.setIgnore(false);
        rpBuilder.setReoptimization(true);
        rpBuilder.setBiDirectional(false);
        rpBuilder.setLoose(true);
        rpBuilder.setMakeBeforeBreak(true);
        rpBuilder.setOrder(false);
        rpBuilder.setPathKey(false);
        rpBuilder.setSupplyOf(false);
        rpBuilder.setFragmentation(false);
        rpBuilder.setP2mp(false);
        rpBuilder.setEroCompression(false);
        rpBuilder.setPriority((short) 5);
        rpBuilder.setRequestId(new RequestId(0xdeadbeefL));
        rpBuilder.setTlvs(new TlvsBuilder().setPathSetupType(new PathSetupTypeBuilder().setPst((short) 1).build()).build());
        return rpBuilder.build();
    }

    private Srp createSrpObject() {
        final SrpBuilder builder = new SrpBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setOperationId(new SrpIdNumber(1L));
        //builder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build());
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder().setPathSetupType(
                new PathSetupTypeBuilder().setPst((short) 1).build()).build());
        return builder.build();
    }

    private Pcerr createErrorMessage(final short type, final short value) {
        final PcerrMessageBuilder errMsgBuilder = new PcerrMessageBuilder();
        errMsgBuilder.setErrors(Lists.newArrayList(new ErrorsBuilder().setErrorObject(
                new ErrorObjectBuilder().setType(type).setValue(value).build()).build()));
        final PcerrBuilder builder = new PcerrBuilder();
        builder.setPcerrMessage(errMsgBuilder.build());
        return builder.build();
    }
}
