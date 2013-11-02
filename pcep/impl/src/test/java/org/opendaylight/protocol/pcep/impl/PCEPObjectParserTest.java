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
import org.opendaylight.protocol.pcep.impl.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSrpObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.PCEPExtensionProviderContextImpl;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.message.c.close.message.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.address.family.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.address.family.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcinitiate.message.pcinitiate.message.requests.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcntf.message.pcntf.message.notifications.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.LoadBalancingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.reported.route.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.GcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public class PCEPObjectParserTest {

	private TlvHandlerRegistry tlvRegistry;

	@Before
	public void setUp() throws Exception {
		this.tlvRegistry = PCEPExtensionProviderContextImpl.create().getTlvHandlerRegistry();
	}

	// @Test
	// @Ignore
	// // FIXME: temporary
	// public void testObjectDeserialization() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
	// PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin"));
	// }
	//
	// @Test
	// public void testUnknownClass() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
	//
	// final PCEPObject obj =
	// PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPObject1UnknownClass.bin")).get(
	// 0);
	//
	// // assertTrue(obj instanceof UnknownObject);
	// // assertEquals(((UnknownObject) obj).getError(), PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
	// }
	//
	// // @Test
	// // public void testUnknownType() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
	// // final PCEPObject obj =
	// PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPObject2UnknownType.bin")).get(0);
	// //
	// // assertTrue(obj instanceof UnknownObject);
	// // assertEquals(((UnknownObject) obj).getError(), PCEPErrors.UNRECOGNIZED_OBJ_TYPE);
	// // }
	// //

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

	//
	// @Test
	// public void testLspObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
	// serDeserTest("src/test/resources/PCEPLspObject1NoTlvsUpperBounds.bin", new PCEPLspObject(0xFFFFF, true, false,
	// true, false, null));
	// }
	//
	// @Test
	// public void testERObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
	// final byte[] bytesFromFile =
	// ByteArray.fileToBytes("src/test/resources/PCEPExplicitRouteObject1PackOfSubobjects.bin");
	//
	// MockitoAnnotations.initMocks(this);
	// PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(registry);
	// doReturn(parser).when(registry).getObjectParser(PCEPExplicitRouteObjectParser.TYPE,
	// PCEPExplicitRouteObjectParser.CLASS);
	// doReturn(new EROAsNumberSubobjectParser()).when(registry).getSubobjectParser(EROAsNumberSubobjectParser.TYPE);
	// ObjectHeader h = new ObjectHeader() {
	//
	// @Override
	// public Class<? extends DataContainer> getImplementedInterface() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public Boolean isProcessingRule() {
	// return false;
	// }
	//
	// @Override
	// public Boolean isIgnore() {
	// return false;
	// }
	// };
	//
	// final ExplicitRouteSubobject specObj = (ExplicitRouteSubobject)
	// registry.getObjectParser(PCEPExplicitRouteObjectParser.TYPE, PCEPExplicitRouteObjectParser.CLASS).parseObject(h,
	// bytesFromFile);
	//
	// System.out.println(specObj.toString());
	//
	// //final byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObj));
	// //assertArrayEquals(bytesFromFile, bytesActual);
	// }
	//
	// @Test
	// public void testIRObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
	// final byte[] bytesFromFile =
	// ByteArray.fileToBytes("src/test/resources/PCEPIncludeRouteObject1PackOfSubobjects.bin");
	//
	// final PCEPIncludeRouteObject specObj = (PCEPIncludeRouteObject)
	// PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
	//
	// assertEquals(8, specObj.getSubobjects().size());
	//
	// final byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObj));
	// assertArrayEquals(bytesFromFile, bytesActual);
	// }
	//
	// @Test
	// public void tesRRObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
	// final byte[] bytesFromFile =
	// ByteArray.fileToBytes("src/test/resources/PCEPReportedRouteObject1PackOfSubobjects.bin");
	//
	// final PCEPReportedRouteObject specObj = (PCEPReportedRouteObject)
	// PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
	//
	// assertEquals(6, specObj.getSubobjects().size());
	//
	// final byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObj));
	// assertArrayEquals(bytesFromFile, bytesActual);
	// }
	//

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

	// @Test
	// public void testNoPathObject() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
	// final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>(1);
	// tlvs.add(new NoPathVectorTlv(false, false, true, false, false, false));
	// serDeserTest("src/test/resources/NoPathObject1WithTLV.bin", new PCEPNoPathObject((short) 2, true, tlvs, false));
	// serDeserTest("src/test/resources/NoPathObject2WithoutTLV.bin", new PCEPNoPathObject((short) 0x10, false, true));
	//
	// byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/NoPathObject2WithoutTLV.bin");
	// PCEPNoPathObject noPathObject = (PCEPNoPathObject) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
	// byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) noPathObject));
	// assertArrayEquals(bytesFromFile, bytesActual);
	//
	// bytesFromFile = ByteArray.fileToBytes("src/test/resources/NoPathObject1WithTLV.bin");
	// noPathObject = (PCEPNoPathObject) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
	// bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) noPathObject));
	// assertArrayEquals(bytesFromFile, bytesActual);
	// }

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

		final NotificationObject t = parser.parseObject(new ObjectHeaderImpl(true, true), result);
		assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result));
		assertArrayEquals(result, parser.serializeObject(builder.build()));
	}

	//
	// /**
	// * Standard ser deser test<br/>
	// * used resources:<br/>
	// * - PCEPOpenObject1.bin
	// *
	// * @throws PCEPDeserializerException
	// * @throws IOException
	// * @throws PCEPDocumentedException
	// */
	// @Test
	// @Ignore
	// // FIXME: temporary
	// public void testOpenObjectSerDeser() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
	// // final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
	// // tlvs.add(new PCEStatefulCapabilityTlv(false, true, true));
	// // tlvs.add(new LSPStateDBVersionTlv(0x80));
	// // final byte[] valueBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC,
	// (byte) 0xDE, (byte) 0xF0 };
	// // tlvs.add(new NodeIdentifierTlv(valueBytes));
	// // final PCEPOpenObject specObject = new PCEPOpenObject(30, 120, 1, tlvs);
	// //
	// // serDeserTest("src/test/resources/PCEPOpenObject1.bin", specObject);
	// }
	//
	// /**
	// * Specific test for upper bounds and without tlvs<br/>
	// * Used resources:<br/>
	// * - PCEPOpenObject2UpperBoundsNoTlv.bin
	// *
	// * @throws PCEPDeserializerException
	// * @throws IOException
	// * @throws PCEPDocumentedException
	// */
	// @Test
	// public void testOpenObjectBoundsWithoutTlvs() throws IOException, PCEPDeserializerException,
	// PCEPDocumentedException {
	// // final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
	// // serDeserTest("src/test/resources/PCEPOpenObject2UpperBoundsNoTlv.bin", new PCEPOpenObject(0xFF, 0xFF, 0xFF,
	// tlvs));
	// serDeserTest("src/test/resources/PCEPOpenObject2UpperBoundsNoTlv.bin", new PCEPOpenObject(0xFF, 0xFF, 0xFF,
	// null));
	// }
	//
	// /**
	// * Standard deserialization test<br/>
	// * Used resources:<br/>
	// * - PCEPRPObject1.bin
	// *
	// * @throws PCEPDeserializerException
	// * @throws IOException
	// * @throws PCEPDocumentedException
	// */
	// @Test
	// public void testRPObjectSerDeser() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
	// serDeserTest("src/test/resources/PCEPRPObject1.bin",
	// new PCEPRequestParameterObject(true, false, true, true, false, false, false, false, (short) 5, 0xdeadbeefL,
	// false, false));
	// // serDeserTest(
	// // "src/test/resources/PCEPRPObject2.bin",
	// // new PCEPRequestParameterObject(true, false, false, false, true, false, true, false, true, (short) 5,
	// 0xdeadbeefL, new ArrayList<PCEPTlv>() {
	// // private static final long serialVersionUID = 1L;
	// //
	// // {
	// // this.add(new OrderTlv(0xFFFFFFFFL, 0x00000001L));
	// // }
	// // }, false, false));
	// }
	//

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

	//
	// /**
	// * Test PCEPExcludeRouteObjectObject (Serialization/Deserialization)<br/>
	// * Used resources:<br/>
	// * - PCEPExcludeRouteObject.1.bin<br/>
	// *
	// * @throws IOException
	// * @throws PCEPDeserializerException
	// * @throws PCEPDocumentedException
	// */
	// @Test
	// public void testExcludeRouteObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
	// final List<ExcludeRouteSubobject> xroSubobjects = new ArrayList<ExcludeRouteSubobject>();
	// xroSubobjects.add(new XROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 192,
	// (byte) 168,
	// (byte) 100, (byte) 100 }), 16), true, XROSubobjectAttribute.NODE));
	// xroSubobjects.add(new XROAsNumberSubobject(new AsNumber(0x1234L), false));
	//
	// }
	//

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

	// // FIXME: add at least one test with true value
	// @Test
	// public void openObjectWithTlv() throws PCEPDeserializerException, PCEPDocumentedException {
	// // this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, false, false));
	// // this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, false, true));
	// // this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, true, false));
	// // this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, true, true));
	// }
	//
	// // private void testOpenObjectWithSpecTlv(final PCEPTlv tlv) throws PCEPDeserializerException,
	// PCEPDocumentedException {
	// // final List<PCEPObject> objs = new ArrayList<PCEPObject>();
	// // final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
	// // tlvs.add(tlv);
	// // final PCEPOpenObject oo = new PCEPOpenObject(30, 120, 0, tlvs);
	// // objs.add(oo);
	// // final byte[] bytes = PCEPObjectFactory.put(objs);
	// // final PCEPObject obj = PCEPObjectFactory.parseObjects(bytes).get(0);
	// // assertEquals(oo, obj);
	// // }
	//
	// @Test
	// public void testErrorsMapping() {
	// final PCEPErrorObjectParser.PCEPErrorsMaping mapper = PCEPErrorObjectParser.PCEPErrorsMaping.getInstance();
	//
	// for (final PCEPErrors error : PCEPErrors.values()) {
	// final PCEPErrorIdentifier errorId = mapper.getFromErrorsEnum(error);
	// assertEquals(error, mapper.getFromErrorIdentifier(errorId));
	// }
	// }
	//
	// @Test
	// public void testOFCodesMapping() {
	// final PCEPOFCodesMapping mapper = PCEPOFCodesMapping.getInstance();
	//
	// for (final PCEPOFCodes ofCode : PCEPOFCodes.values()) {
	// final int ofCodeId = mapper.getFromOFCodesEnum(ofCode);
	// assertEquals(ofCode, mapper.getFromCodeIdentifier(ofCodeId));
	// }
	// }
	//
	// @SuppressWarnings("unchecked")
	// private static <T extends PCEPObject> void serDeserTestWithoutBin(final T object) throws
	// PCEPDeserializerException,
	// PCEPDocumentedException {
	// final byte[] serBytes = PCEPObjectFactory.put(Arrays.asList((PCEPObject) object));
	// final T deserObj = (T) PCEPObjectFactory.parseObjects(serBytes).get(0);
	//
	// assertEquals(object, deserObj);
	// }
	//
	// @Test
	// public void testSERObjects() throws PCEPDocumentedException, PCEPDeserializerException {
	// final List<ExplicitRouteSubobject> eroSubobjects = new ArrayList<ExplicitRouteSubobject>();
	// eroSubobjects.add(new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 192,
	// (byte) 168, 1, 8 }), 16), false));
	// eroSubobjects.add(new EROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(new byte[] { (byte) 192,
	// (byte) 168, 2, 1,
	// (byte) 192, (byte) 168, 2, 1, (byte) 192, (byte) 168, 2, 1, (byte) 192, (byte) 168, 2, 1 }), 64), false));
	//
	// serDeserTestWithoutBin(new PCEPSecondaryExplicitRouteObject(eroSubobjects, true, false));
	// }
	//
	// @Test
	// public void testSRRObject() throws PCEPDocumentedException, PCEPDeserializerException {
	// final List<ReportedRouteSubobject> rroSubobjects = new ArrayList<ReportedRouteSubobject>();
	// rroSubobjects.add(new RROIPAddressSubobject<IPv4Prefix>(new IPv4Prefix(this.ipv4addr, 16), true, false));
	// rroSubobjects.add(new RROIPAddressSubobject<IPv6Prefix>(new IPv6Prefix(this.ipv6addr, 64), false, true));
	//
	// serDeserTestWithoutBin(new PCEPSecondaryRecordRouteObject(rroSubobjects, true, false));
	// }
	//
	// @Test
	// public void testP2MPEndpointsObjects() throws PCEPDeserializerException, PCEPDocumentedException {
	// serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv4Address>(2, this.ipv4addr, Arrays.asList(this.ipv4addr,
	// this.ipv4addr,
	// this.ipv4addr), true, false));
	// serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv4Address>(1, this.ipv4addr, Arrays.asList(this.ipv4addr),
	// true, false));
	// serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv6Address>(2, this.ipv6addr, Arrays.asList(this.ipv6addr,
	// this.ipv6addr,
	// this.ipv6addr), true, false));
	// serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv6Address>(1, this.ipv6addr, Arrays.asList(this.ipv6addr),
	// true, false));
	// }
	//
	// @Test
	// public void testUnreachedDestinationObjects() throws PCEPDeserializerException, PCEPDocumentedException {
	// serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv4Address>(Arrays.asList(this.ipv4addr,
	// this.ipv4addr, this.ipv4addr), true, false));
	// serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv4Address>(Arrays.asList(this.ipv4addr), true,
	// false));
	// serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv6Address>(Arrays.asList(this.ipv6addr,
	// this.ipv6addr, this.ipv6addr), true, false));
	// serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv6Address>(Arrays.asList(this.ipv6addr), true,
	// false));
	// }
}
