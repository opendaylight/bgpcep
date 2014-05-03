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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.crabbe.initiated00.InitiatedActivator;
import org.opendaylight.protocol.pcep.crabbe.initiated00.PcinitiateMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02PCReportMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02PCUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.Activator;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PlspId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;

import com.google.common.collect.Lists;

public class PCEPMessageTest {

	private Lspa lspa;
	private Metrics metrics;
	private Ero ero;
	private EndpointsObj endpoints;
	private AsNumberCase eroASSubobject;
	private Activator act;
	private Lsp lsp;

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
	}

	@Test
	public void testPcinitMsg() throws IOException, PCEPDeserializerException {
		try (InitiatedActivator a = new InitiatedActivator()) {
			a.start(this.ctx);
			final byte[] result = ByteArray.fileToBytes("src/test/resources/Pcinit.bin");

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

			assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			final ByteBuf buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());
		}
	}

	@Test
	public void testUpdMsg() throws IOException, PCEPDeserializerException {
		try (StatefulActivator a = new StatefulActivator()) {
			a.start(this.ctx);
			byte[] result = ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin");

			final Stateful02PCUpdateRequestMessageParser parser = new Stateful02PCUpdateRequestMessageParser(this.ctx.getObjectHandlerRegistry());

			final PcupdMessageBuilder builder = new PcupdMessageBuilder();

			final List<Updates> updates = Lists.newArrayList();
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder();
			pBuilder.setEro(this.ero);
			pBuilder.setLspa(this.lspa);
			updates.add(new UpdatesBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
			builder.setUpdates(updates);

			assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			ByteBuf buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());

			result = ByteArray.fileToBytes("src/test/resources/PCUpd.5.bin");

			final List<Updates> updates1 = Lists.newArrayList();
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder();
			pBuilder1.setEro(this.ero);
			pBuilder1.setLspa(this.lspa);
			updates1.add(new UpdatesBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
			updates1.add(new UpdatesBuilder().setLsp(this.lsp).setPath(pBuilder1.build()).build());
			builder.setUpdates(updates1);

			assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());
		}
	}

	@Test
	public void testRptMsg() throws IOException, PCEPDeserializerException {
		try (StatefulActivator a = new StatefulActivator()) {
			a.start(this.ctx);
			byte[] result = ByteArray.fileToBytes("src/test/resources/PCRpt.1.bin");

			final Stateful02PCReportMessageParser parser = new Stateful02PCReportMessageParser(this.ctx.getObjectHandlerRegistry());

			final PcrptMessageBuilder builder = new PcrptMessageBuilder();

			final List<Reports> reports = Lists.newArrayList();
			reports.add(new ReportsBuilder().setLsp(this.lsp).build());
			builder.setReports(reports);

			assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			ByteBuf buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());

			result = ByteArray.fileToBytes("src/test/resources/PCRpt.2.bin");

			final List<Reports> reports1 = Lists.newArrayList();
			reports1.add(new ReportsBuilder().setLsp(this.lsp).setPath(
					new PathBuilder().setEro(
							this.ero).setLspa(this.lspa).build()).build());
			builder.setReports(reports1);

			assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());

			result = ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin");

			final List<Reports> reports2 = Lists.newArrayList();
			final PathBuilder pBuilder = new PathBuilder();
			pBuilder.setEro(this.ero);
			pBuilder.setLspa(this.lspa);
			pBuilder.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
			reports2.add(new ReportsBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
			builder.setReports(reports2);

			assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());

			result = ByteArray.fileToBytes("src/test/resources/PCRpt.5.bin");

			final List<Reports> reports3 = Lists.newArrayList();
			final PathBuilder pBuilder1 = new PathBuilder();
			pBuilder1.setEro(this.ero);
			pBuilder1.setLspa(this.lspa);
			pBuilder1.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
			reports3.add(new ReportsBuilder().setLsp(this.lsp).setPath(pBuilder.build()).build());
			reports3.add(new ReportsBuilder().setLsp(this.lsp).setPath(pBuilder1.build()).build());
			builder.setReports(reports3);

			assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
					parser.parseMessage(ByteArray.cutBytes(result, 4), Collections.<Message> emptyList()));
			buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());
		}
	}
}
