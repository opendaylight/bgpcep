/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.initiated00.PcinitiateMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPReportMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

import com.google.common.collect.Lists;

public class PCEPValidatorTest {

	private ObjectHandlerRegistry objectRegistry;

	private Rp rpFalse;
	private Open open;
	private Lspa lspa;
	private Metrics metrics;
	private Iro iro;
	private Ero ero;
	private Rro rro;
	private Srp srp;
	private Lsp lsp;

	private AsNumberCase eroASSubobject;
	private UnnumberedCase rroUnnumberedSub;

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

		this.rroUnnumberedSub = new UnnumberedCaseBuilder().setUnnumbered(
				new UnnumberedBuilder().setRouterId(0x00112233L).setInterfaceId(0x00ff00ffL).build()).build();

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

		final RroBuilder rroBuilder = new RroBuilder();
		rroBuilder.setIgnore(false);
		rroBuilder.setProcessingRule(false);
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject> rroSubs = Lists.newArrayList();
		rroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setSubobjectType(
				this.rroUnnumberedSub).setProtectionAvailable(false).setProtectionInUse(false).build());
		rroBuilder.setSubobject(rroSubs);
		this.rro = rroBuilder.build();

		final SrpBuilder srpBuilder = new SrpBuilder();
		srpBuilder.setIgnore(false);
		srpBuilder.setProcessingRule(false);
		srpBuilder.setOperationId(new SrpIdNumber(1L));
		srpBuilder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(false).build());
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
		lspBuilder.setTlvs(new TlvsBuilder().build());
		lspBuilder.addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(false).build());
		this.lsp = lspBuilder.build();

		final Ipv4Builder afi = new Ipv4Builder();
		afi.setSourceIpv4Address(new Ipv4Address("255.255.255.255"));
		afi.setDestinationIpv4Address(new Ipv4Address("255.255.255.255"));
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
		final Stateful tlv1 = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).build();
		b.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().addAugmentation(
				Tlvs2.class, new Tlvs2Builder().setStateful(tlv1).build()).build());
		builder.setOpen(b.build());

		assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		final ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}

	@Test
	public void testUpdMsg() throws IOException, PCEPDeserializerException {
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin");

		final PCEPUpdateRequestMessageParser parser = new PCEPUpdateRequestMessageParser(this.objectRegistry);

		final PcupdMessageBuilder builder = new PcupdMessageBuilder();

		final List<Updates> updates = Lists.newArrayList();
		final PathBuilder pBuilder = new PathBuilder();
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
		final PathBuilder pBuilder1 = new PathBuilder();
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
		reports1.add(new ReportsBuilder().setLsp(this.lsp).setPath(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder().setEro(
						this.ero).setLspa(this.lspa).build()).build());
		builder.setReports(reports1);

		assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin");

		final List<Reports> reports2 = Lists.newArrayList();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder();
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
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder();
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
	public void testErrorMsg() throws IOException, PCEPDeserializerException {
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCErr.3.bin");

		final ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

		final PCEPErrorMessageParser parser = new PCEPErrorMessageParser(this.objectRegistry);

		List<Errors> innerErr = Lists.newArrayList();
		innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

		final PcerrMessageBuilder builder = new PcerrMessageBuilder();
		builder.setErrors(innerErr);
		builder.setErrorType(new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(this.open).build()).build());

		assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		ByteBuf buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());

		result = ByteArray.fileToBytes("src/test/resources/PCErr.5.bin");

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.Rps> rps = Lists.newArrayList();
		rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder().setRp(
				this.rpFalse).build());

		innerErr = Lists.newArrayList();
		innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

		builder.setErrors(innerErr);
		builder.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(rps).build()).build());

		assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
				parser.parseMessage(result, Collections.<Message> emptyList()));
		buf = Unpooled.buffer(result.length);
		parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
		assertArrayEquals(result, buf.array());
	}
}
