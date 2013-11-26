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
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReportMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PcinitiateMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcupdBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedBuilder;

import com.google.common.collect.Lists;

public class PCEPValidatorTest {

	private ObjectHandlerRegistry objectRegistry;

	private Rp rpTrue;
	private Rp rpFalse;
	private Open open;
	private NoPath noPath;
	private Lspa lspa;
	private Bandwidth bandwidth;
	private Metrics metrics;
	private Iro iro;
	private Ero ero;
	private Rro rro;
	private Of of;
	private Srp srp;
	private Lsp lsp;
	private EndpointsObj endpoints;
	private Svec svec;

	private AsNumber eroASSubobject;
	private Unnumbered rroUnnumberedSub;

	@Before
	public void setUp() throws Exception {
		this.objectRegistry = ServiceLoaderPCEPExtensionProviderContext.create().getObjectHandlerRegistry();
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
		bBuilder.setBandwidth(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(new Float32(new byte[4])));
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

		this.rroUnnumberedSub = new UnnumberedBuilder().setRouterId(0x00112233L).setInterfaceId(0x00ff00ffL).build();

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

		final RroBuilder rroBuilder = new RroBuilder();
		rroBuilder.setIgnore(false);
		rroBuilder.setProcessingRule(false);
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobjects> rroSubs = Lists.newArrayList();
		rroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectsBuilder().setSubobjectType(
				this.rroUnnumberedSub).setProtectionAvailable(false).setProtectionInUse(false).build());
		rroBuilder.setSubobjects(rroSubs);
		this.rro = rroBuilder.build();

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

		final Ipv4Builder afi = new Ipv4Builder();
		afi.setSourceIpv4Address(new Ipv4Address("255.255.255.255"));
		afi.setDestinationIpv4Address(new Ipv4Address("255.255.255.255"));

		final EndpointsObjBuilder epBuilder = new EndpointsObjBuilder();
		epBuilder.setIgnore(false);
		epBuilder.setProcessingRule(true);
		epBuilder.setAddressFamily(afi.build());
		this.endpoints = epBuilder.build();

		final SvecBuilder sBuilder = new SvecBuilder();
		sBuilder.setIgnore(false);
		sBuilder.setProcessingRule(false);
		sBuilder.setLinkDiverse(false);
		sBuilder.setNodeDiverse(false);
		sBuilder.setSrlgDiverse(false);
		sBuilder.setRequestsIds(Lists.newArrayList(new RequestId(1L)));
		this.svec = sBuilder.build();
	}

	@Test
	public void testOpenMsg() throws IOException, PCEPDeserializerException {
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

		assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testKeepAliveMsg() throws IOException, PCEPDeserializerException {
		final byte[] result = new byte[] {};
		final PCEPKeepAliveMessageParser parser = new PCEPKeepAliveMessageParser(this.objectRegistry);
		final KeepaliveBuilder builder = new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build());

		assertEquals(builder.build(), parser.parseMessage(result, Collections.<Message> emptyList()));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(builder.build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testCloseMsg() throws IOException, PCEPDeserializerException {
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPCloseMessage1.bin");

		final PCEPCloseMessageParser parser = new PCEPCloseMessageParser(this.objectRegistry);
		final CloseBuilder builder = new CloseBuilder().setCCloseMessage(new CCloseMessageBuilder().setCClose(
				new CCloseBuilder().setIgnore(false).setProcessingRule(false).setReason((short) 5).build()).build());

		assertEquals(builder.build(), parser.parseMessage(result, Collections.<Message> emptyList()));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(builder.build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testRequestMsg() throws IOException, PCEPDeserializerException {
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPRequestMessage1.bin");

		final PCEPRequestMessageParser parser = new PCEPRequestMessageParser(this.objectRegistry);

		final PcreqMessageBuilder builder = new PcreqMessageBuilder();
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests> reqs1 = Lists.newArrayList();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder rBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder();
		rBuilder.setRp(this.rpTrue);
		rBuilder.setSegmentComputation(new SegmentComputationBuilder().setP2p(new P2pBuilder().setEndpointsObj(this.endpoints).build()).build());
		reqs1.add(rBuilder.build());
		builder.setRequests(reqs1);

		assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCReq.3.bin");

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests> reqs2 = Lists.newArrayList();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder rBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder();
		rBuilder1.setRp(this.rpTrue);
		final P2pBuilder p2pBuilder = new P2pBuilder();
		p2pBuilder.setEndpointsObj(this.endpoints);
		p2pBuilder.setLsp(this.lsp);
		p2pBuilder.setMetrics(Lists.newArrayList(this.metrics));
		p2pBuilder.setIro(this.iro);
		rBuilder1.setSegmentComputation(new SegmentComputationBuilder().setP2p(p2pBuilder.build()).build());
		reqs2.add(rBuilder1.build());
		builder.setRequests(reqs2);
		builder.setSvec(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder().setSvec(
				this.svec).build()));

		assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// specMessages.clear();
		// requests = new ArrayList<CompositeRequestObject>();
		// requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new
		// IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, null));
		// specMessages.add(new PCEPRequestMessage(requests));
		//
		// final byte[] ipAdress3 = { (byte) 0x7F, (byte) 0x00, (byte) 0x30, (byte) 0x01 };
		// requests = new ArrayList<CompositeRequestObject>();
		// requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(false, false, false, false, false,
		// false,
		// false, false, (short) 4, 1, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress3),
		// new
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
		// requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(true, false, false, false, false,
		// false,
		// false, false, (short) 1, 1, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress5),
		// new
		// IPv4Address(ipAdress5)), null, null, null, null, null, null, null, null, null));
		//
		// specMessages.add(new PCEPRequestMessage(requests));
		// deserMsgs = deserMsg("src/test/resources/PCReq.4.bin");
		// assertEquals(deserMsgs.toString(), specMessages.toString());
		//
		// specMessages.clear();
		// svecList = new ArrayList<CompositeRequestSvecObject>();
		// svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(true, false, false, false, false,
		// this.requestIds,
		// false)));
		// svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(false, true, true, false, false,
		// this.requestIds,
		// false), new PCEPObjectiveFunctionObject(PCEPOFCodes.MCC, true, false), new
		// PCEPGlobalConstraintsObject((short)
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
	}

	@Test
	public void testReplyMsg() throws IOException, PCEPDeserializerException {
		// only RP
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCRep.1.bin");

		final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(this.objectRegistry);

		final PcrepMessageBuilder builder = new PcrepMessageBuilder();
		RepliesBuilder rBuilder = new RepliesBuilder();

		final List<Replies> replies1 = Lists.newArrayList();
		rBuilder.setRp(this.rpTrue);
		replies1.add(rBuilder.build());
		builder.setReplies(replies1);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// simple Failure
		result = ByteArray.fileToBytes("src/test/resources/PCRep.2.bin");
		final List<Replies> replies2 = Lists.newArrayList();
		rBuilder = new RepliesBuilder();
		rBuilder.setRp(this.rpTrue);
		replies2.add(rBuilder.build());
		final RepliesBuilder rBuilder2 = new RepliesBuilder();
		rBuilder2.setRp(this.rpTrue);
		rBuilder2.setResult(new FailureBuilder().setNoPath(this.noPath).build());
		replies2.add(rBuilder2.build());
		builder.setReplies(replies2);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// Failure with attributes
		result = ByteArray.fileToBytes("src/test/resources/PCRep.3.bin");
		final List<Replies> replies3 = Lists.newArrayList();
		rBuilder = new RepliesBuilder();
		rBuilder.setRp(this.rpTrue);
		rBuilder.setResult(new FailureBuilder().setNoPath(this.noPath).setLspa(this.lspa).setMetrics(Lists.newArrayList(this.metrics)).setIro(
				this.iro).build());
		replies3.add(rBuilder.build());
		builder.setReplies(replies3);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		// Success
		result = ByteArray.fileToBytes("src/test/resources/PCRep.5.bin");
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
		rBuilder.setResult(new SuccessBuilder().setPaths(paths).build()).build();
		replies4.add(rBuilder.build());
		builder.setReplies(replies4);

		assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testUpdMsg() throws IOException, PCEPDeserializerException {
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin");

		final PCEPUpdateRequestMessageParser parser = new PCEPUpdateRequestMessageParser(this.objectRegistry);

		final PcupdMessageBuilder builder = new PcupdMessageBuilder();

		final List<Updates> updates = Lists.newArrayList();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.PathBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.PathBuilder();
		pBuilder.setEro(this.ero);
		pBuilder.setLspa(this.lspa);
		updates.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lsp).setPath(pBuilder.build()).build());
		builder.setUpdates(updates);

		assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCUpd.5.bin");

		final List<Updates> updates1 = Lists.newArrayList();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.PathBuilder pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.PathBuilder();
		pBuilder1.setEro(this.ero);
		pBuilder1.setLspa(this.lspa);
		updates1.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lsp).setPath(pBuilder.build()).build());
		updates1.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lsp).setPath(pBuilder1.build()).build());
		builder.setUpdates(updates1);

		assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testRptMsg() throws IOException, PCEPDeserializerException {
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCRpt.1.bin");

		final PCEPReportMessageParser parser = new PCEPReportMessageParser(this.objectRegistry);

		final PcrptMessageBuilder builder = new PcrptMessageBuilder();

		final List<Reports> reports = Lists.newArrayList();
		reports.add(new ReportsBuilder().setLsp(this.lsp).build());
		builder.setReports(reports);

		assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCRpt.2.bin");

		final List<Reports> reports1 = Lists.newArrayList();
		reports1.add(new ReportsBuilder().setLsp(this.lsp).setPath(new PathBuilder().setEro(this.ero).setLspa(this.lspa).build()).build());
		builder.setReports(reports1);

		assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin");

		final List<Reports> reports2 = Lists.newArrayList();
		final PathBuilder pBuilder = new PathBuilder();
		pBuilder.setEro(this.ero);
		pBuilder.setLspa(this.lspa);
		pBuilder.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
		pBuilder.setRro(this.rro);
		reports2.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lsp).setPath(pBuilder.build()).build());
		builder.setReports(reports2);

		assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCRpt.5.bin");

		final List<Reports> reports3 = Lists.newArrayList();
		final PathBuilder pBuilder1 = new PathBuilder();
		pBuilder1.setEro(this.ero);
		pBuilder1.setLspa(this.lspa);
		pBuilder1.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
		pBuilder1.setRro(this.rro);
		reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lsp).setPath(pBuilder.build()).build());
		reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lsp).setPath(pBuilder1.build()).build());
		builder.setReports(reports3);

		assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testPcinitMsg() throws IOException, PCEPDeserializerException {
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

		assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
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
		rps1.add(new RpsBuilder().setRp(this.rpFalse).build());
		rps1.add(new RpsBuilder().setRp(this.rpFalse).build());

		b.setNotifications(innerNot1);
		b.setRps(rps1);
		nots.add(b.build());
		builder.setNotifications(nots);

		assertEquals(new PcntfBuilder().setPcntfMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcntfBuilder().setPcntfMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testErrorMsg() throws IOException, PCEPDeserializerException {
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCErr.3.bin");

		final ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

		final PCEPErrorMessageParser parser = new PCEPErrorMessageParser(this.objectRegistry);

		List<Errors> innerErr = Lists.newArrayList();
		innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

		final PcerrMessageBuilder builder = new PcerrMessageBuilder();
		builder.setErrors(innerErr);
		builder.setErrorType(new SessionBuilder().setOpen(this.open).build());

		assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCErr.5.bin");

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.Rps> rps = Lists.newArrayList();
		rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request.RpsBuilder().setRp(
				this.rpFalse).build());

		innerErr = Lists.newArrayList();
		innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

		builder.setErrors(innerErr);
		builder.setErrorType(new RequestBuilder().setRps(rps).build());

		assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}
}
