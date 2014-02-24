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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
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
	private Iro iro;
	private Ero ero;
	private EndpointsObj endpoints;
	private AsNumberCase eroASSubobject;
	private Activator act;

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
					parser.parseMessage(result, Collections.<Message> emptyList()));
			final ByteBuf buf = Unpooled.buffer(result.length);
			parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
			assertArrayEquals(result, buf.array());
		}
	}
}
