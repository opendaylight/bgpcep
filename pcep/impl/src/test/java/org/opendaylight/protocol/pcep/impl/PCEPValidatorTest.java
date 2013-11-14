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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.impl.message.PcinitiateMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.PCEPExtensionProviderContextImpl;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.LspBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.PcntfMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberBuilder;

import com.google.common.collect.Lists;

public class PCEPValidatorTest {

	private ObjectHandlerRegistry objectRegistry;

	private Rp rp;
	private Open open;
	private NoPath noPath;
	private Lspa lspa;
	private Bandwidth bandwidth;
	private Metrics metrics;
	private Iro iro;
	private Ero ero;
	private Of of;
	private Srp srp;
	private Lsp lsp;

	private AsNumber eroASSubobject;

	@Before
	public void setUp() throws Exception {
		this.objectRegistry = PCEPExtensionProviderContextImpl.create().getObjectHandlerRegistry();
		final RpBuilder rpBuilder = new RpBuilder();
		rpBuilder.setProcessingRule(false);
		rpBuilder.setIgnore(false);
		rpBuilder.setReoptimization(false);
		rpBuilder.setBiDirectional(false);
		rpBuilder.setLoose(true);
		rpBuilder.setMakeBeforeBreak(false);
		rpBuilder.setOrder(false);
		rpBuilder.setSupplyOf(false);
		rpBuilder.setFragmentation(false);
		rpBuilder.setP2mp(false);
		rpBuilder.setEroCompression(false);
		rpBuilder.setPriority((short) 1);
		rpBuilder.setRequestId(new RequestId(10L));
		this.rp = rpBuilder.build();

		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder openBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder();
		openBuilder.setProcessingRule(false);
		openBuilder.setIgnore(false);
		openBuilder.setDeadTimer((short) 0);
		openBuilder.setKeepalive((short) 0);
		openBuilder.setSessionId((short) 0);
		openBuilder.setVersion(new ProtocolVersion((short) 1));
		openBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().build());
		this.open = openBuilder.build();

		final NoPathBuilder npbuilder = new NoPathBuilder();
		npbuilder.setProcessingRule(false);
		npbuilder.setIgnore(false);
		npbuilder.setNatureOfIssue((short) 0);
		npbuilder.setUnsatisfiedConstraints(false);
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
		this.lspa = lspaBuilder.build();

		final BandwidthBuilder bBuilder = new BandwidthBuilder();
		bBuilder.setIgnore(false);
		bBuilder.setProcessingRule(false);
		bBuilder.setBandwidth(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth(new Float32(new byte[4])));
		this.bandwidth = bBuilder.build();

		final MetricBuilder mBuilder = new MetricBuilder();
		mBuilder.setIgnore(false);
		mBuilder.setProcessingRule(false);
		mBuilder.setComputed(false);
		mBuilder.setBound(false);
		mBuilder.setMetricType((short) 1);
		mBuilder.setValue(new Float32(new byte[4]));
		this.metrics = new MetricsBuilder().setMetric(mBuilder.build()).build();

		this.eroASSubobject = new AsNumberBuilder().setAsNumber(
				new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber(0xFFFFL)).build();

		final IroBuilder iroBuilder = new IroBuilder();
		iroBuilder.setIgnore(false);
		iroBuilder.setProcessingRule(false);
		final List<Subobjects> iroSubs = Lists.newArrayList();
		iroSubs.add(new SubobjectsBuilder().setSubobjectType(this.eroASSubobject).build());
		iroBuilder.setSubobjects(iroSubs);
		this.iro = iroBuilder.build();

		final EroBuilder eroBuilder = new EroBuilder();
		eroBuilder.setIgnore(false);
		eroBuilder.setProcessingRule(false);
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects> eroSubs = Lists.newArrayList();
		eroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectsBuilder().setSubobjectType(
				this.eroASSubobject).setLoose(false).build());
		eroBuilder.setSubobjects(eroSubs);
		this.ero = eroBuilder.build();

		final OfBuilder ofBuilder = new OfBuilder();
		ofBuilder.setIgnore(false);
		ofBuilder.setProcessingRule(false);
		ofBuilder.setCode(new OfId(0));
		this.of = ofBuilder.build();

		final SrpBuilder srpBuilder = new SrpBuilder();
		srpBuilder.setIgnore(false);
		srpBuilder.setProcessingRule(false);
		srpBuilder.setOperationId(new SrpIdNumber(1L));
		this.srp = srpBuilder.build();

		final LspBuilder lspBuilder = new LspBuilder();
		lspBuilder.setIgnore(false);
		lspBuilder.setProcessingRule(false);
		lspBuilder.setAdministrative(false);
		lspBuilder.setDelegate(false);
		lspBuilder.setPlspId(new PlspId(0L));
		lspBuilder.setOperational(OperationalStatus.Down);
		lspBuilder.setSync(false);
		lspBuilder.setRemove(false);
		lspBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.lsp.TlvsBuilder().build());
		this.lsp = lspBuilder.build();
	}

	// private static final LspaObject lspa = new PCEPLspaObject(0L, 0L, 0L, (short) 0, (short) 0, false, false, false,
	// false);
	//
	// private final List<ExplicitRouteSubobject> eroSubobjects = asList(
	// new EROAsNumberSubobject(new AsNumber(0xFFFFL), false),
	// new EROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte)
	// 0x33 }), new UnnumberedInterfaceIdentifier(0x00FF00FF), false));
	//
	// private final List<ReportedRouteSubobject> rroSubobjects = asList((ReportedRouteSubobject) new
	// RROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] {
	// (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33 }), new UnnumberedInterfaceIdentifier(0x00FF00FF)));
	//
	// private final List<Long> requestIds = asList(0x000001L);
	//
	// private final IPv4Address ip4addr = new IPv4Address(new byte[] { (byte) 0xFF, 0x00, 0x00, 0x01 });
	//
	// private final PCEPSvecObject svecObj = new PCEPSvecObject(true, true, true, false, false,
	// PCEPValidatorTest.this.requestIds, true);
	//
	// private final PCEPRequestParameterObject requestParameter = new PCEPRequestParameterObject(true, false, false,
	// false, false, false, false, false, (short) 3, 1, true, false);
	//
	// // private final PCEPEndPointsObject<IPv4Address> endPoints = new
	// // PCEPEndPointsObject<IPv4Address>(this.ip4addr, this.ip4addr);
	//
	// private final PCEPEndPointsObject<IPv4Address> endPoints = new PCEPEndPointsObject<IPv4Address>(this.ip4addr,
	// this.ip4addr);
	//

	@Test
	public void testOpenMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPOpenMessage1.bin");
		final PCEPOpenMessageParser parser = new PCEPOpenMessageParser(this.objectRegistry);
		final OpenMessageBuilder builder = new OpenMessageBuilder();

		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder();
		b.setProcessingRule(false);
		b.setIgnore(false);
		b.setVersion(new ProtocolVersion((short) 1));
		b.setKeepalive((short) 30);
		b.setDeadTimer((short) 120);
		b.setSessionId((short) 1);
		final Stateful tlv1 = new StatefulBuilder().setFlags(new Flags(true, false, true)).build();
		final LspDbVersion tlv2 = new LspDbVersionBuilder().setVersion(BigInteger.valueOf(0x80L)).build();
		b.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().setStateful(
				tlv1).setLspDbVersion(tlv2).build());
		builder.setOpen(b.build());

		assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(), parser.parseMessage(result));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testKeepAliveMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] result = new byte[] { 0, 0, 0, 0 };
		final PCEPKeepAliveMessageParser parser = new PCEPKeepAliveMessageParser(this.objectRegistry);
		final KeepaliveBuilder builder = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build());

		assertEquals(builder.build(), parser.parseMessage(result));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(builder.build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testCloseMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPCloseMessage1.bin");

		final PCEPCloseMessageParser parser = new PCEPCloseMessageParser(this.objectRegistry);
		final CloseBuilder builder = new CloseBuilder().setCCloseMessage(new CCloseMessageBuilder().setCClose(
				new CCloseBuilder().setIgnore(false).setProcessingRule(false).setReason((short) 5).build()).build());

		assertEquals(builder.build(), parser.parseMessage(result));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(builder.build(), buf);
		assertArrayEquals(result, buf.array());
	}

	// @Test
	// public void testRequestMessageValidationFromBin() throws IOException, PCEPDeserializerException,
	// PCEPDocumentedException,
	// DeserializerException, DocumentedException {
	// List<CompositeRequestObject> requests = new ArrayList<CompositeRequestObject>();
	// final byte[] ipAdress = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	// requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(true, false, false, false, false, false,
	// false, false, (short) 5, 0xDEADBEEFL, true, false), new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress), new IPv4Address(ipAdress))));
	// PCEPRequestMessage specMessage = new PCEPRequestMessage(requests);
	// List<Message> deserMsgs = deserMsg("src/test/resources/PCEPRequestMessage1.bin");
	// final List<Message> specMessages = Lists.newArrayList();
	// specMessages.add(specMessage);
	//
	// assertEquals(deserMsgs.toString(), specMessages.toString());
	//
	// requests = new ArrayList<CompositeRequestObject>();
	// final byte[] ipAdress2 = { (byte) 0x7F, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress2), new IPv4Address(ipAdress2))));
	// specMessage = new PCEPRequestMessage(requests);
	// deserMsgs = deserMsg("src/test/resources/PCReq.1.bin");
	// specMessages.clear();
	// specMessages.add(specMessage);
	// assertEquals(deserMsgs.toString(), specMessages.toString());
	//
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, new
	// PCEPLoadBalancingObject(3, new Bandwidth(ByteArray.floatToBytes((float) 1024.75)), false)));
	// specMessage = new PCEPRequestMessage(requests);
	// deserMsgs = deserMsg("src/test/resources/PCReq.2.bin");
	// specMessages.clear();
	// specMessages.add(specMessage);
	// assertEquals(deserMsgs.toString(), specMessages.toString());
	//
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, new PCEPLspObject(1, false, false, true, false),
	// PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(1000)), false,
	// false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, true, new IGPMetric(53L), false, false));
	// }
	// }, new PCEPReportedRouteObject(this.rroSubobjects, false), new PCEPExistingPathBandwidthObject(new
	// Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPIncludeRouteObject(this.eroSubobjects, false,
	// false), new PCEPLoadBalancingObject(5, new Bandwidth(ByteArray.floatToBytes(3)), false)));
	//
	// List<CompositeRequestSvecObject> svecList = new ArrayList<CompositeRequestSvecObject>();
	// svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(true, false, false, false, false, this.requestIds,
	// false)));
	//
	// specMessage = new PCEPRequestMessage(svecList, requests);
	// deserMsgs = deserMsg("src/test/resources/PCReq.3.bin");
	// specMessages.clear();
	// specMessages.add(specMessage);
	// // FIXME BUG-89
	// // assertEquals(deserMsgs, specMessages);
	//
	// specMessages.clear();
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, null));
	// specMessages.add(new PCEPRequestMessage(requests));
	//
	// final byte[] ipAdress3 = { (byte) 0x7F, (byte) 0x00, (byte) 0x30, (byte) 0x01 };
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(false, false, false, false, false, false,
	// false, false, (short) 4, 1, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress3), new
	// IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, null));
	// specMessages.add(new PCEPRequestMessage(requests));
	//
	// final byte[] ipAdress4 = { (byte) 0x7F, (byte) 0x30, (byte) 0x00, (byte) 0x01 };
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress2), new IPv4Address(ipAdress4)), null, null, null, null, null, null, null, null, null));
	// specMessages.add(new PCEPRequestMessage(requests));
	//
	// final byte[] ipAdress5 = { (byte) 0x7F, (byte) 0xd0, (byte) 0x00, (byte) 0x01 };
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(true, false, false, false, false, false,
	// false, false, (short) 1, 1, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress5), new
	// IPv4Address(ipAdress5)), null, null, null, null, null, null, null, null, null));
	//
	// specMessages.add(new PCEPRequestMessage(requests));
	// deserMsgs = deserMsg("src/test/resources/PCReq.4.bin");
	// assertEquals(deserMsgs.toString(), specMessages.toString());
	//
	// specMessages.clear();
	// svecList = new ArrayList<CompositeRequestSvecObject>();
	// svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(true, false, false, false, false, this.requestIds,
	// false)));
	// svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(false, true, true, false, false, this.requestIds,
	// false), new PCEPObjectiveFunctionObject(PCEPOFCodes.MCC, true, false), new PCEPGlobalConstraintsObject((short)
	// 0x55, (short) 1, (short) 100, (short) 0x26, true, false), new PCEPExcludeRouteObject(new
	// ArrayList<ExcludeRouteSubobject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new XROAsNumberSubobject(new AsNumber((long) 0x12), true));
	// }
	// }, true, true, false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, true, new TEMetric(123456L), true, false));
	// }
	// }));
	//
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, PCEPValidatorTest.lspa, new
	// PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(1000)), false, false), new
	// ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, true, new IGPMetric(53L), false, false));
	// this.add(new PCEPMetricObject(true, true, new IGPMetric(5335L), false, false));
	// this.add(new PCEPMetricObject(true, true, new IGPMetric(128256), false, false));
	// }
	// }, new PCEPReportedRouteObject(this.rroSubobjects, false), new PCEPExistingPathBandwidthObject(new
	// Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPIncludeRouteObject(this.eroSubobjects, false,
	// false), new PCEPLoadBalancingObject(5, new Bandwidth(ByteArray.floatToBytes(3)), false)));
	//
	// final byte[] ipAdress6 = { (byte) 0x7F, (byte) 0xF0, (byte) 0x00, (byte) 0x01 };
	// specMessages.add(new PCEPRequestMessage(svecList, requests));
	//
	// requests = new ArrayList<CompositeRequestObject>();
	// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
	// IPv4Address(ipAdress6), new IPv4Address(ipAdress6)), null, null, PCEPValidatorTest.lspa, new
	// PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(1000)), false, false), new
	// ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, true, new IGPMetric(53L), false, false));
	// }
	// }, new PCEPReportedRouteObject(this.rroSubobjects, false), new PCEPExistingPathBandwidthObject(new
	// Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPIncludeRouteObject(this.eroSubobjects, false,
	// false), new PCEPLoadBalancingObject(5, new Bandwidth(ByteArray.floatToBytes(3f)), false)));
	// deserMsgs = deserMsg("src/test/resources/PCReq.5.bin");
	// specMessages.add(new PCEPRequestMessage(svecList, requests));
	// // FIXME
	// // assertEquals(deserMsgs, specMessages);
	//
	// // FIXME: need construct with invalid processed parameter
	// // assertEquals(deserMsg("src/test/resources/PCReq.6.invalid.bin"),
	// // asList(
	// // new PCEPErrorMessage(new CompositeErrorObject(new
	// // PCEPRequestParameterObject(true, false, false, false, false, false,
	// // false, false, (short) 3,
	// // 1L, false, false), new PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))),
	// // new PCEPRequestMessage(asList(new
	// // CompositeRequestObject(this.requestParameter, new
	// // PCEPEndPointsObject<IPv4Address>(IPv4Address
	// // .getNetworkAddressFactory().getNetworkAddressForBytes(new byte[] {
	// // 127, 0, 0, 1 }), IPv4Address.getNetworkAddressFactory()
	// // .getNetworkAddressForBytes(new byte[] { 127, 0, 0, 1 })), null, null,
	// // null, null, null, null, null, null, new PCEPLoadBalancingObject(
	// // 3, new Bandwidth(1024.75), false))))));
	//
	// }
	//
	// @Test
	// public void testRequestMessageValidationFromRawMsg() throws PCEPDeserializerException {
	// List<PCEPObject> objs = new ArrayList<PCEPObject>();
	// List<Message> msgs;
	// PCEPRequestParameterObject tmpRP;
	//
	// // test unrecognized object in svec list
	// objs.add(this.svecObj);
	// objs.add(new UnknownObject(true, false, PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
	// objs.add(new PCEPSvecObject(true, true, true, false, false, PCEPValidatorTest.this.requestIds, true));
	//
	// msgs = PCEPMessageValidator.getValidator(PCEPMessageType.REQUEST).validate(objs);
	//
	// assertEquals(msgs.get(0).toString(), new PCEPErrorMessage(new ArrayList<PCEPErrorObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
	// }
	// }).toString());
	//
	// // test with request p flag not set and ignoracion of more than one
	// // end-points objects
	// objs = new ArrayList<PCEPObject>();
	// objs.add(this.svecObj);
	// objs.add(this.svecObj);
	// tmpRP = new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1,
	// false, false);
	// objs.add(tmpRP);
	// objs.add(this.endPoints);
	//
	// objs.add(this.requestParameter);
	// objs.add(this.endPoints);
	// objs.add(this.endPoints);
	// // FIXME:mv use object constructor with set processed flag
	// // objs.add(this.classTypeProvider);
	// // objs.add(this.requestParameter);
	// // objs.add(this.endPointsProvider);
	// // objs.add(new PCEPClassTypeObjectProvider((short) 7, false));
	//
	// msgs = PCEPMessageValidator.getValidator(PCEPMessageType.REQUEST).validate(objs);
	// // FIXME:mv use object constructor with set processed flag
	// // assertEquals(msgs.get(0), new PCEPErrorMessage(new
	// // CompositeErrorObject(tmpRP, new
	// // PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
	// // assertEquals(
	// // msgs.get(1),
	// // new PCEPRequestMessage(asList(new
	// // CompositeRequestSvecObject(this.svecObj), new
	// // CompositeRequestSvecObject(this.svecObj)), Util
	// // .asList(new CompositeRequestObject(this.requestParameter,
	// // this.endPoints, this.classType, null, null, null, null, null, null,
	// // null,
	// // null))));
	// // assertEquals(msgs.get(2), new PCEPErrorMessage(new
	// // CompositeErrorObject(tmpRP, new
	// // PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
	// }
	//

	@Test
	public void testReplyMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		// only RP
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCRep.1.bin");

		final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(this.objectRegistry);

		final PcrepMessageBuilder builder = new PcrepMessageBuilder();
		RepliesBuilder rBuilder = new RepliesBuilder();

		final List<Replies> replies1 = Lists.newArrayList();
		rBuilder.setRp(this.rp);
		replies1.add(rBuilder.build());
		builder.setReplies(replies1);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// simple Failure
		result = ByteArray.fileToBytes("src/test/resources/PCRep.2.bin");
		final List<Replies> replies2 = Lists.newArrayList();
		rBuilder = new RepliesBuilder();
		rBuilder.setRp(this.rp);
		replies2.add(rBuilder.build());
		final RepliesBuilder rBuilder2 = new RepliesBuilder();
		rBuilder2.setRp(this.rp);
		rBuilder2.setResult(new FailureBuilder().setNoPath(this.noPath).build());
		replies2.add(rBuilder2.build());
		builder.setReplies(replies2);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// Failure with attributes
		result = ByteArray.fileToBytes("src/test/resources/PCRep.3.bin");
		final List<Replies> replies3 = Lists.newArrayList();
		rBuilder = new RepliesBuilder();
		rBuilder.setRp(this.rp);
		rBuilder.setResult(new FailureBuilder().setNoPath(this.noPath).setLspa(this.lspa).setMetrics(Lists.newArrayList(this.metrics)).setIro(
				this.iro).build());
		replies3.add(rBuilder.build());
		builder.setReplies(replies3);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// Success
		result = ByteArray.fileToBytes("src/test/resources/PCRep.5.bin");
		final List<Replies> replies4 = Lists.newArrayList();
		rBuilder = new RepliesBuilder();
		rBuilder.setRp(this.rp);
		final List<Paths> paths = Lists.newArrayList();
		final PathsBuilder paBuilder = new PathsBuilder();
		paBuilder.setEro(this.ero);
		paBuilder.setLspa(this.lspa);
		paBuilder.setMetrics(Lists.newArrayList(this.metrics));
		paBuilder.setIro(this.iro);
		paBuilder.setOf(this.of);
		paths.add(paBuilder.build());
		rBuilder.setResult(new SuccessBuilder().setPaths(paths).build()).build();
		replies4.add(rBuilder.build());
		builder.setReplies(replies4);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	//
	// @Test
	// public void testUpdMessageValidatorFromBin() throws IOException, PCEPDeserializerException,
	// PCEPDocumentedException,
	// DeserializerException, DocumentedException {
	// List<Message> specMessages = Lists.newArrayList();
	//
	// List<CompositeUpdateRequestObject> requests = new ArrayList<CompositeUpdateRequestObject>();
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
	//
	// specMessages.add(new PCEPUpdateRequestMessage(requests));
	// assertEquals(deserMsg("src/test/resources/PCUpd.1.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// List<CompositeUpdPathObject> paths = new ArrayList<CompositeUpdPathObject>();
	// paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, null, null));
	// requests = new ArrayList<CompositeUpdateRequestObject>();
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
	// specMessages.add(new PCEPUpdateRequestMessage(requests));
	// assertEquals(deserMsg("src/test/resources/PCUpd.2.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// paths = new ArrayList<CompositeUpdPathObject>();
	// paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// requests = new ArrayList<CompositeUpdateRequestObject>();
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
	// specMessages.add(new PCEPUpdateRequestMessage(requests));
	// assertEquals(deserMsg("src/test/resources/PCUpd.3.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// requests = new ArrayList<CompositeUpdateRequestObject>();
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
	// specMessages.add(new PCEPUpdateRequestMessage(requests));
	// assertEquals(deserMsg("src/test/resources/PCUpd.4.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// requests = new ArrayList<CompositeUpdateRequestObject>();
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
	// paths = new ArrayList<CompositeUpdPathObject>();
	// paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
	// paths = new ArrayList<CompositeUpdPathObject>();
	// paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
	// specMessages.add(new PCEPUpdateRequestMessage(requests));
	// assertEquals(deserMsg("src/test/resources/PCUpd.5.bin").toString(), specMessages.toString());
	// }
	//
	// @Test
	// public void testRptMessageValidatorFromBin() throws IOException, PCEPDeserializerException,
	// PCEPDocumentedException,
	// DeserializerException, DocumentedException {
	// List<Message> specMessages = Lists.newArrayList();
	// List<CompositeStateReportObject> reports = new ArrayList<CompositeStateReportObject>();
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
	// specMessages.add(new PCEPReportMessage(reports));
	// assertEquals(deserMsg("src/test/resources/PCRpt.1.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// List<CompositeRptPathObject> paths = new ArrayList<CompositeRptPathObject>();
	// paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, null, null, null));
	// reports = new ArrayList<CompositeStateReportObject>();
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
	// specMessages.add(new PCEPReportMessage(reports));
	// assertEquals(deserMsg("src/test/resources/PCRpt.2.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// paths = new ArrayList<CompositeRptPathObject>();
	// paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	//
	// reports = new ArrayList<CompositeStateReportObject>();
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
	// specMessages.add(new PCEPReportMessage(reports));
	//
	// // FIXME
	// // assertEquals(deserMsg("src/test/resources/PCRpt.3.bin"), specMessages);
	//
	// specMessages = Lists.newArrayList();
	// reports = new ArrayList<CompositeStateReportObject>();
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
	// specMessages.add(new PCEPReportMessage(reports));
	// assertEquals(deserMsg("src/test/resources/PCRpt.4.bin").toString(), specMessages.toString());
	//
	// specMessages = Lists.newArrayList();
	// reports = new ArrayList<CompositeStateReportObject>();
	// paths = new ArrayList<CompositeRptPathObject>();
	// paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
	// paths = new ArrayList<CompositeRptPathObject>();
	// paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false),
	// PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false,
	// false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
	// private static final long serialVersionUID = 1L;
	//
	// {
	// this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
	// }
	// }));
	// reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
	// specMessages.add(new PCEPReportMessage(reports));
	// // FIXME
	// // assertEquals(deserMsg("src/test/resources/PCRpt.5.bin").toString(), specMessages.toString());
	// }
	//

	@Test
	public void testPcinitMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] result = ByteArray.fileToBytes("src/test/resources/Pcinit.bin");

		final PcinitiateMessageParser parser = new PcinitiateMessageParser(this.objectRegistry);

		final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
		final RequestsBuilder rBuilder = new RequestsBuilder();

		final List<Requests> reqs = Lists.newArrayList();
		rBuilder.setSrp(this.srp);
		rBuilder.setLsp(this.lsp);
		rBuilder.setEro(this.ero);
		rBuilder.setLspa(this.lspa);
		rBuilder.setMetrics(Lists.newArrayList(this.metrics));
		rBuilder.setIro(this.iro);
		reqs.add(rBuilder.build());
		builder.setRequests(reqs);

		final Message m = parser.parseMessage(result);
		assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), parser.parseMessage(result));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testNotificationMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final CNotification cn1 = new CNotificationBuilder().setIgnore(false).setProcessingRule(false).setType((short) 1).setValue(
				(short) 1).build();

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.Notifications> innerNot = Lists.newArrayList();
		innerNot.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder().setCNotification(
				cn1).build());
		final List<Rps> rps = Lists.newArrayList();
		rps.add(new RpsBuilder().setRp(this.rp).build());

		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCNtf.5.bin");

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
		rps1.add(new RpsBuilder().setRp(this.rp).build());
		rps1.add(new RpsBuilder().setRp(this.rp).build());

		b.setNotifications(innerNot1);
		b.setRps(rps1);
		nots.add(b.build());
		builder.setNotifications(nots);

		assertEquals(new PcntfBuilder().setPcntfMessage(builder.build()).build(), parser.parseMessage(result));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcntfBuilder().setPcntfMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testErrorMsg() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

		final PCEPErrorMessageParser parser = new PCEPErrorMessageParser(this.objectRegistry);

		List<Errors> innerErr = Lists.newArrayList();
		innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

		final PcerrMessageBuilder builder = new PcerrMessageBuilder();
		builder.setErrors(innerErr);
		builder.setErrorType(new SessionBuilder().setOpen(this.open).build());

		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCErr.3.bin");

		assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		final byte[] result1 = ByteArray.fileToBytes("src/test/resources/PCErr.5.bin");

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.Rps> rps = Lists.newArrayList();
		rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.RpsBuilder().setRp(
				this.rp).build());

		innerErr = Lists.newArrayList();
		innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

		builder.setErrors(innerErr);
		builder.setErrorType(new RequestBuilder().setRps(rps).build());

		assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result1));
		buf = Unpooled.buffer(result1.length);
		parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
		assertArrayEquals(result1, buf.array());
	}
}
