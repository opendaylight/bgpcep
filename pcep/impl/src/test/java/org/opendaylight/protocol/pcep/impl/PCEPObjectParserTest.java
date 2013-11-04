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

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.impl.object.PCEPBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPathKeyObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSrpObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.PCEPExtensionProviderContextImpl;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.IncludeRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.open.message.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.definition.ExplicitRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.path.key.expansion.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.LoadBalancingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.reported.route.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.GcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.XroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedBuilder;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public class PCEPObjectParserTest {

	private TlvHandlerRegistry tlvRegistry;

	@Before
	public void setUp() throws Exception {
		this.tlvRegistry = PCEPExtensionProviderContextImpl.create().getTlvHandlerRegistry();
	}

	@Test
	public void testOpenObjectWithTLV() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		final PCEPOpenObjectParser parser = new PCEPOpenObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin");

		final OpenBuilder builder = new OpenBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		builder.setVersion(new ProtocolVersion((short) 1));
		builder.setKeepalive((short) 30);
		builder.setDeadTimer((short) 120);
		builder.setSessionId((short) 1);

		final Stateful tlv1 = new StatefulBuilder().setFlags(new Flags(true, false, true)).build();
		final LspDbVersion tlv2 = new LspDbVersionBuilder().setVersion(BigInteger.valueOf(0x80L)).build();
		final byte[] predundancyBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde,
				(byte) 0xf0 };
		final PredundancyGroupId tlv3 = new PredundancyGroupIdBuilder().setIdentifier(predundancyBytes).build();

		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.TlvsBuilder().setStateful(
				tlv1).setPredundancyGroupId(tlv3).setLspDbVersion(tlv2).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testCloseObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPCloseObjectParser parser = new PCEPCloseObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPCloseObject1.bin");

		final CCloseBuilder builder = new CCloseBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		builder.setReason((short) 5);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testLoadBalancingObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPLoadBalancingObjectParser parser = new PCEPLoadBalancingObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPLoadBalancingObject1.bin");

		final LoadBalancingBuilder builder = new LoadBalancingBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		builder.setMaxLsp((short) UnsignedBytes.toInt((byte) 0xf1));
		builder.setMinBandwidth(new Float32(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testLspObjectWithTLV() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPLspObjectParser parser = new PCEPLspObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPLspObject1WithTLV.bin");

		final LspBuilder builder = new LspBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setAdministrative(true);
		builder.setDelegate(false);
		builder.setRemove(true);
		builder.setSync(false);
		builder.setOperational(OperationalStatus.GoingDown);
		builder.setPlspId(new PlspId(0x12345L));

		final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(627610883L).build();
		final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SymbolicPathName("Med".getBytes())).build();
		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.TlvsBuilder().setLspErrorCode(
				tlv1).setSymbolicPathName(tlv2).build());
		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testERObject() throws Exception {
		final PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(PCEPExtensionProviderContextImpl.create().getEROSubobjectHandlerRegistry());
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPExplicitRouteObject1PackOfSubobjects.bin");

		final ExplicitRouteBuilder builder = new ExplicitRouteBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects> subs = Lists.newArrayList();
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder().setLoose(
				true).setSubobjectType(new AsNumberBuilder().setAsNumber(new AsNumber(0xffffL)).build()).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder().setLoose(
				true).setSubobjectType(new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder().setLoose(
				true).setSubobjectType(new UnnumberedBuilder().setRouterId(0xffffffffL).setInterfaceId(0xffffffffL).build()).build());
		builder.setSubobjects(subs);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testIRObject() throws Exception {
		final PCEPIncludeRouteObjectParser parser = new PCEPIncludeRouteObjectParser(PCEPExtensionProviderContextImpl.create().getEROSubobjectHandlerRegistry());
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPIncludeRouteObject1PackOfSubobjects.bin");
		final byte[] ip6PrefixBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		final IncludeRouteBuilder builder = new IncludeRouteBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Subobjects> subs = Lists.newArrayList();
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.SubobjectsBuilder().setSubobjectType(
				new AsNumberBuilder().setAsNumber(new AsNumber(0x10L)).build()).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.SubobjectsBuilder().setSubobjectType(
				new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("18.52.80.0/21"))).build()).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.SubobjectsBuilder().setSubobjectType(
				new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ip6PrefixBytes, 22))).build()).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.SubobjectsBuilder().setSubobjectType(
				new UnnumberedBuilder().setRouterId(0x1245678L).setInterfaceId(0x9abcdef0L).build()).build());
		builder.setSubobjects(subs);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		// FIXME: fix Ipv6Serializers (getType)
		// assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testRRObject() throws Exception {
		final PCEPReportedRouteObjectParser parser = new PCEPReportedRouteObjectParser(PCEPExtensionProviderContextImpl.create().getRROSubobjectHandlerRegistry());
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPReportedRouteObject1PackOfSubobjects.bin");
		final byte[] ip6PrefixBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		final ReportedRouteBuilder builder = new ReportedRouteBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Subobjects> subs = Lists.newArrayList();
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.SubobjectsBuilder().setSubobjectType(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixBuilder().setIpPrefix(
						new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).setProtectionAvailable(false).setProtectionInUse(false).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.SubobjectsBuilder().setSubobjectType(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixBuilder().setIpPrefix(
						new IpPrefix(Ipv6Util.prefixForBytes(ip6PrefixBytes, 22))).build()).setProtectionAvailable(false).setProtectionInUse(
				false).build());
		subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.SubobjectsBuilder().setSubobjectType(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedBuilder().setRouterId(
						0x1245678L).setInterfaceId(0x9abcdef0L).build()).setProtectionAvailable(false).setProtectionInUse(false).build());
		builder.setSubobjects(subs);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		// FIXME: fix Ipv6Serializers (getType)
		// assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testBandwidthObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPBandwidthObjectParser parser = new PCEPBandwidthObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPBandwidthObject1LowerBounds.bin");

		final BandwidthBuilder builder = new BandwidthBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setBandwidth(new Float32(result));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPBandwidthObject2UpperBounds.bin");

		builder.setBandwidth(new Float32(result));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testEndPointsObjectIPv4() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] srcIPBytes = { (byte) 0xA2, (byte) 0xF5, (byte) 0x11, (byte) 0x0E };
		final byte[] destIPBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		final PCEPEndPointsObjectParser parser = new PCEPEndPointsObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPEndPointsObject1IPv4.bin");

		final EndpointsBuilder builder = new EndpointsBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		builder.setAddressFamily(new Ipv4Builder().setSourceIpv4Address(Ipv4Util.addressForBytes(srcIPBytes)).setDestinationIpv4Address(
				Ipv4Util.addressForBytes(destIPBytes)).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testEndPointsObjectIPv6() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] destIPBytes = { (byte) 0x00, (byte) 0x02, (byte) 0x5D, (byte) 0xD2, (byte) 0xFF, (byte) 0xEC, (byte) 0xA1,
				(byte) 0xB6, (byte) 0x58, (byte) 0x1E, (byte) 0x9F, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, };
		final byte[] srcIPBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		final PCEPEndPointsObjectParser parser = new PCEPEndPointsObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPEndPointsObject2IPv6.bin");

		final EndpointsBuilder builder = new EndpointsBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		builder.setAddressFamily(new Ipv6Builder().setSourceIpv6Address(Ipv6Util.addressForBytes(srcIPBytes)).setDestinationIpv6Address(
				Ipv6Util.addressForBytes(destIPBytes)).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testErrorObjectWithTlv() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		final PCEPErrorObjectParser parser = new PCEPErrorObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPErrorObject1.bin");

		final ErrorsBuilder builder = new ErrorsBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setType((short) 1);
		builder.setValue((short) 1);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPErrorObject3.bin");

		builder.setType((short) 7);
		builder.setValue((short) 0);
		builder.setTlvs(new TlvsBuilder().setReqMissing(new ReqMissingBuilder().setRequestId(new RequestId(0x00001155L)).build()).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testLspaObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPLspaObjectParser parser = new PCEPLspaObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPLspaObject1LowerBounds.bin");

		final LspaBuilder builder = new LspaBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setExcludeAny(new AttributeFilter(0L));
		builder.setIncludeAny(new AttributeFilter(0L));
		builder.setIncludeAll(new AttributeFilter(0L));
		builder.setHoldPriority((short) 0);
		builder.setSetupPriority((short) 0);
		builder.setLocalProtectionDesired(false);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPLspaObject2UpperBounds.bin");

		builder.setExcludeAny(new AttributeFilter(0xFFFFFFFFL));
		builder.setIncludeAny(new AttributeFilter(0xFFFFFFFFL));
		builder.setIncludeAll(new AttributeFilter(0xFFFFFFFFL));
		builder.setHoldPriority((short) 0xFF);
		builder.setSetupPriority((short) 0xFF);
		builder.setLocalProtectionDesired(true);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPLspaObject3RandVals.bin");

		builder.setExcludeAny(new AttributeFilter(0x20A1FEE3L));
		builder.setIncludeAny(new AttributeFilter(0x1A025CC7L));
		builder.setIncludeAll(new AttributeFilter(0x2BB66532L));
		builder.setHoldPriority((short) 0x02);
		builder.setSetupPriority((short) 0x03);
		builder.setLocalProtectionDesired(true);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testMetricObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPMetricObjectParser parser = new PCEPMetricObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPMetricObject1LowerBounds.bin");

		final MetricBuilder builder = new MetricBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setComputed(false);
		builder.setBound(false);
		builder.setMetricType((short) 1);
		builder.setValue(new Float32(new byte[] { 0, 0, 0, 0 }));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPMetricObject2UpperBounds.bin");

		builder.setComputed(true);
		builder.setBound(false);
		builder.setMetricType((short) 2);
		builder.setValue(new Float32(new byte[] { (byte) 0x4f, (byte) 0x70, (byte) 0x00, (byte) 0x00 }));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testNoPathObjectWithTlv() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		final PCEPNoPathObjectParser parser = new PCEPNoPathObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPNoPathObject1WithoutTLV.bin");

		final NoPathBuilder builder = new NoPathBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setNatureOfIssue((short) 1);
		builder.setUnsatisfiedConstraints(true);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPNoPathObject2WithTLV.bin");

		builder.setNatureOfIssue((short) 0);
		builder.setUnsatisfiedConstraints(false);

		final NoPathVectorBuilder b = new NoPathVectorBuilder();
		b.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags(false, true, false, true, false, true, true, true));
		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.no.path.TlvsBuilder().setNoPathVector(
				b.build()).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testNotifyObjectWithTlv() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		final PCEPNotificationObjectParser parser = new PCEPNotificationObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPNotificationObject2WithoutTlv.bin");

		final NotificationsBuilder builder = new NotificationsBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setType((short) 0xff);
		builder.setValue((short) 0xff);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPNotificationObject1WithTlv.bin");

		builder.setType((short) 2);
		builder.setValue((short) 1);
		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.TlvsBuilder().setOverloadDuration(
				new OverloadDurationBuilder().setDuration(0xff0000a2L).build()).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testRPObjectWithTlv() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		final PCEPRequestParameterObjectParser parser = new PCEPRequestParameterObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPRPObject1.bin");

		final RpBuilder builder = new RpBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(true);
		builder.setReoptimization(true);
		builder.setBiDirectional(false);
		builder.setLoose(true);
		builder.setMakeBeforeBreak(true);
		builder.setOrder(false);
		builder.setSupplyOf(false);
		builder.setFragmentation(false);
		builder.setP2mp(false);
		builder.setEroCompression(false);
		builder.setPriority((short) 5);
		builder.setRequestId(new RequestId(0xdeadbeefL));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPRPObject2.bin");

		builder.setReoptimization(false);
		builder.setFragmentation(true);
		builder.setEroCompression(true);

		final OrderBuilder b = new OrderBuilder();
		b.setDelete(0xffffffffL);
		b.setSetup(1L);

		builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.TlvsBuilder().setOrder(
				b.build()).build());

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testSvecObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPSvecObjectParser parser = new PCEPSvecObjectParser(this.tlvRegistry);
		byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPSvecObject2.bin");

		final SvecBuilder builder = new SvecBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		builder.setLinkDiverse(false);
		builder.setNodeDiverse(false);
		builder.setSrlgDiverse(false);
		builder.setRequestsIds(Lists.newArrayList(new RequestId(0xFFL)));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));

		result = ByteArray.fileToBytes("src/test/resources/PCEPSvecObject1_10ReqIDs.bin");

		builder.setProcessingRule(true);
		builder.setLinkDiverse(true);
		builder.setSrlgDiverse(true);

		final List<RequestId> requestIDs = Lists.newArrayList();
		requestIDs.add(new RequestId(0xFFFFFFFFL));
		requestIDs.add(new RequestId(0x00000000L));
		requestIDs.add(new RequestId(0x01234567L));
		requestIDs.add(new RequestId(0x89ABCDEFL));
		requestIDs.add(new RequestId(0xFEDCBA98L));
		requestIDs.add(new RequestId(0x76543210L));
		requestIDs.add(new RequestId(0x15825266L));
		requestIDs.add(new RequestId(0x48120BBEL));
		requestIDs.add(new RequestId(0x25FB7E52L));
		requestIDs.add(new RequestId(0xB2F2546BL));

		builder.setRequestsIds(requestIDs);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testClassTypeObject() throws PCEPDeserializerException, PCEPDocumentedException {
		final PCEPClassTypeObjectParser parser = new PCEPClassTypeObjectParser(this.tlvRegistry);
		final byte[] result = new byte[] { 0, 0, 0, (byte) 0x04 };

		final ClassTypeBuilder builder = new ClassTypeBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		builder.setClassType(new ClassType((short) 4));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testExcludeRouteObject() throws Exception {
		final PCEPExcludeRouteObjectParser parser = new PCEPExcludeRouteObjectParser(PCEPExtensionProviderContextImpl.create().getXROSubobjectHandlerRegistry());
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPExcludeRouteObject.1.bin");

		final XroBuilder builder = new XroBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExcludeRouteObject.Flags(true));
		final List<Subobjects> subs = Lists.newArrayList();
		subs.add(new SubobjectsBuilder().setMandatory(true).setSubobjectType(
				new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("192.168.0.0/16"))).build()).setAttribute(Attribute.Node).build());
		subs.add(new SubobjectsBuilder().setMandatory(false).setSubobjectType(
				new AsNumberBuilder().setAsNumber(new AsNumber(0x1234L)).build()).build());
		builder.setSubobjects(subs);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testPathKeyObject() throws Exception {
		final PCEPPathKeyObjectParser parser = new PCEPPathKeyObjectParser(PCEPExtensionProviderContextImpl.create().getEROSubobjectHandlerRegistry());
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPPathKeyObject.bin");

		final PathKeyBuilder builder = new PathKeyBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		final List<PathKeys> list = Lists.newArrayList();
		list.add(new PathKeysBuilder().setLoose(true).setPathKey(new PathKey(0x1234)).setPceId(
				new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 })).build());
		builder.setPathKeys(list);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testSrpObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPSrpObjectParser parser = new PCEPSrpObjectParser(this.tlvRegistry);
		final byte[] result = new byte[] { 0, 0, 0, 0, 0, 0, 0, (byte) 0x01 };

		final SrpBuilder builder = new SrpBuilder();
		builder.setProcessingRule(false);
		builder.setIgnore(false);
		builder.setOperationId(new SrpIdNumber(1L));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testObjectiveFunctionObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPObjectiveFunctionObjectParser parser = new PCEPObjectiveFunctionObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPObjectiveFunctionObject.1.bin");

		final OfBuilder builder = new OfBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		builder.setCode(new OfId(4));

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	@Test
	public void testGlobalConstraintsObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final PCEPGlobalConstraintsObjectParser parser = new PCEPGlobalConstraintsObjectParser(this.tlvRegistry);
		final byte[] result = ByteArray.fileToBytes("src/test/resources/PCEPGlobalConstraintsObject.1.bin");

		final GcBuilder builder = new GcBuilder();
		builder.setProcessingRule(true);
		builder.setIgnore(false);
		builder.setMaxHop((short) 1);
		builder.setMaxUtilization((short) 0);
		builder.setMinUtilization((short) 100);
		builder.setOverBookingFactor((short) 0xFF);

		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}
}
