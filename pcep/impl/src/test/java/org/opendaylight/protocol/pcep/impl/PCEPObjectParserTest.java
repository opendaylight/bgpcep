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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.any;
import static org.mockito.Mockito.doReturn;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.concepts.IGPMetric;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser.PCEPErrorIdentifier;
import org.opendaylight.protocol.pcep.impl.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.AsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.object.PCEPBranchNodeListObject;
import org.opendaylight.protocol.pcep.object.PCEPClassTypeObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPGlobalConstraintsObject;
import org.opendaylight.protocol.pcep.object.PCEPIncludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPObjectiveFunctionObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.object.PCEPP2MPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPSecondaryExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPSecondaryRecordRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPSvecObject;
import org.opendaylight.protocol.pcep.object.PCEPUnreachedDestinationObject;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSubobjectAttribute;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Used resources<br/>
 * <br/>
 * PCEPOpenObject3.bin<br/>
 * objClass: 1<br/>
 * objType: 1<br/>
 * objLength: 8<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * keepAlive: 30<br/>
 * deadTimer: 120<br/>
 * sessionId: 1<br/>
 * tlvs:NO<br/>
 * <br/>
 * PCEPBandwidthObject1LowerBounds.bin<br/>
 * objClass: 5 <br/>
 * objType: 1<br/>
 * objLength: 16<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * Bandwidth: 0<br/>
 * <br/>
 * PCEPBandwidthObject2UpperBounds.bin<br/>
 * objClass: 5 <br/>
 * objType: 1<br/>
 * objLength: 16<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * Bandwidth: 0xFFFFFFFF<br/>
 * <br/>
 * PCEPEndPointsObject1IPv4.bin<br/>
 * objClass: 4 <br/>
 * objType: 1<br/>
 * objLength: 12<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * src IP: 0xA2F5110E <br/>
 * dest IP: 0xFFFFFFFF <br/>
 * <br/>
 * PCEPEndPointsObject2IPv6.bin<br/>
 * objClass: 4 <br/>
 * objType: 2<br/>
 * objLength: 36<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * src IP: 0xFFFFFFFFF FFFFFFFFF FFFFFFFFF FFFFFFFFF<br/>
 * dest IP: 0x00025DD2 FFECA1B6 581E9F50 00000000 <br/>
 * <br/>
 * PCEPErrorObject1.bin<br/>
 * objClass: 13 (RP)<br/>
 * objType: 1<br/>
 * objLength: 8<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * Error-type: 1<br/>
 * Error-value: 1<br/>
 * Tlvs: NO<br/>
 * <br/>
 * PCEPErrorObject2Invalid.bin<br/>
 * objClass: 13 (RP)<br/>
 * objType: 1<br/>
 * objLength: 8<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * Error-type: 3<br/>
 * Error-value: 0<br/>
 * Tlvs: NO<br/>
 * <br/>
 * PCEPErrorObject3.bin<br/>
 * objClass: 13 (RP)<br/>
 * objType: 1<br/>
 * objLength: 8<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * Error-type: 2<br/>
 * Error-value: 0<br/>
 * Tlvs: NO<br/>
 * <br/>
 * PCEPLspaObject1LowerBounds.bin<br/>
 * objClass: 9<br/>
 * objType: 1<br/>
 * objLength: 20<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * Exclude-any: 0x00000000L<br/>
 * Include-any: 0x00000000L<br/>
 * Include-all: 0x00000000L<br/>
 * Setup Prio: 0x00<br/>
 * Holding Prio: 0x00<br/>
 * Flags: - L : false<br/>
 * <br/>
 * PCEPLspaObject2UpperBounds.bin<br/>
 * objClass: 9<br/>
 * objType: 1<br/>
 * objLength: 20<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * Exclude-any: 0xFFFFFFFFL<br/>
 * Include-any: 0xFFFFFFFFL<br/>
 * Include-all: 0xFFFFFFFFL<br/>
 * Setup Prio: 0xFF<br/>
 * Holding Prio: 0xFF<br/>
 * Flags: - L : true<br/>
 * <br/>
 * PCEPLspaObject3RandVals.bin<br/>
 * objClass: 9<br/>
 * objType: 1<br/>
 * objLength: 20<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: true<br/>
 * <br/>
 * Exclude-any: 0x20A1FEE3L<br/>
 * Include-any: 0x1A025CC7L<br/>
 * Include-all: 0x2BB66532L<br/>
 * Setup Prio: 0x03<br/>
 * Holding Prio: 0x02<br/>
 * Flags: - L : true<br/>
 * <br/>
 * NoPathObject1WithTLV.bin<br/>
 * objClass: 3 (RP)<br/>
 * objType: 1<br/>
 * objLength: 16<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * Nature of Issue: 2<br/>
 * No-Path flags:<br/>
 * - C: true<br/>
 * <br/>
 * tlvs:<br/>
 * -- NO-PATH-VECTOR<br/>
 * - flags (0x4000):<br/>
 * - PCE currently unavailable: false<br/>
 * - unknown destination: true<br/>
 * - unknown source: false<br/>
 * 
 * <br/>
 * NoPathObject2WithoutTLV.bin<br/>
 * objClass: 3 (RP)<br/>
 * objType: 1<br/>
 * objLength: 8<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: true<br/>
 * <br/>
 * Nature of Issue: 16<br/>
 * No-Path flags:<br/>
 * - C: false<br/>
 * <br/>
 * tlvs:NO<br/>
 * <br/>
 * PCEPNotificationObject1WithTlv.bin <br/>
 * objClass: 12<br/>
 * objType: 1<br/>
 * objLength: 16<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * NT: 1<br/>
 * NV: 1<br/>
 * Tlvs:<br/>
 * - OverloaderDuration(0xFF0000A2L)<br/>
 * <br/>
 * PCEPNotificationObject2WithoutTlv.bin <br/>
 * objClass: 12<br/>
 * objType: 1<br/>
 * objLength: 8<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * NT: 0xFF<br/>
 * NV: 0xFF<br/>
 * Tlvs: NO<br/>
 * <br/>
 * PCEPOpenObject1.bin<br/>
 * objClass: 1<br/>
 * objType: 1<br/>
 * objLength: 28<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * keepAlive: 30<br/>
 * deadTimer: 120<br/>
 * sessionId: 1<br/>
 * tlvs:<br/>
 * - PCEPStatefulCapability<br/>
 * - LSPStateDBVersionTlv<br/>
 * - NodeIdentifierTlv<br/>
 * <br/>
 * PCEPOpenObject2UpperBoundsNoTlv.bin<br/>
 * objClass: 1<br/>
 * objType: 1<br/>
 * objLength: 34<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * keepAlive: 0xFF<br/>
 * deadTimer: 0xFF<br/>
 * sessionId: 0xFF<br/>
 * tlvs: NO<br/>
 * <br/>
 * PCEPRPObject1.bin<br/>
 * objClass: 2 (RP)<br/>
 * objType: 1<br/>
 * objLength: 12<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * RP flags:<br/>
 * - loose/strict: true<br/>
 * - Bi-directional: false<br/>
 * - Reoptimization: false<br/>
 * - Priority: 5<br/>
 * Request ID: 0xDEADBEEF<br/>
 * tlvs: NO<br/>
 * <br/>
 * PCEPSvecObject1_10ReqIDs.bin <br/>
 * objClass: 11<br/>
 * objType: 1<br/>
 * objLength: 48<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: true<br/>
 * - ignored: false<br/>
 * <br/>
 * Flags:<br/>
 * - Link diverse: true<br/>
 * - Node diverse: false<br/>
 * - SRLG diverse: true<br/>
 * Reques-ID-numbers:<br/>
 * #1 - 0xFFFFFFFFL<br/>
 * #2 - 0x00000000L<br/>
 * #3 - 0x01234567L<br/>
 * #4 - 0x89ABCDEFL<br/>
 * #5 - 0xFEDCBA98L<br/>
 * #6 - 0x76543210L<br/>
 * #7 - 0x15825266L<br/>
 * #8 - 0x48120BBEL<br/>
 * #9 - 0x25FB7E52L<br/>
 * #10 - 0xB2F2546BL<br/>
 * <br/>
 * PCEPSvecObject2.bin <br/>
 * objClass: 11<br/>
 * objType: 1<br/>
 * objLength: 08<br/>
 * version: 1<br/>
 * Flags:<br/>
 * - processing: false<br/>
 * - ignored: false<br/>
 * <br/>
 * Flags:<br/>
 * - Link diverse: false<br/>
 * - Node diverse: false<br/>
 * - SRLG diverse: false<br/>
 * Reques-ID-numbers:<br/>
 * #1 - 0x000000FFL<br/>
 * PCEPExcludeRouteObject.1.bin <br/>
 * objClass: 17 <br/>
 * objType: 1 <br/>
 * objLength: 20 <br/>
 * version: 1 <br/>
 * Flags: <br/>
 * - fail: true <br/>
 * Subobjects: <br/>
 * - XROIPv4PreffixSubobject(192.168.0.0/16, exclude, node) <br/>
 * - XROASnumber(0x1234) <br/>
 */
public class PCEPObjectParserTest {
	
	@Mock
	private HandlerRegistry registry;

	IPv4Address ipv4addr = new IPv4Address(new byte[] { (byte) 192, (byte) 168, 1, 8 });

	IPv6Address ipv6addr = new IPv6Address(new byte[] { (byte) 192, (byte) 168, 2, 1, (byte) 192, (byte) 168, 2, 1, (byte) 192, (byte) 168,
			2, 1, (byte) 192, (byte) 168, 2, 1 });

	@SuppressWarnings("unchecked")
	private static <T extends PCEPObject> void serDeserTest(final String srcFile, final T specObject) throws IOException,
			PCEPDeserializerException, PCEPDocumentedException {
		final byte[] bytesFromFile = ByteArray.fileToBytes(srcFile);
		final T deserSpecObj = (T) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
		final byte[] serSpecObj = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObject));

		assertEquals(specObject, deserSpecObj);
		assertArrayEquals(bytesFromFile, serSpecObj);
	}

	/**
	 * Standard serialization test<br/>
	 * Used resources:<br/>
	 * - PCEPOpenObject1.bin<br/>
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	@Ignore
	// FIXME: temporary
	public void testObjectDeserialization() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin"));
	}

	@Test
	public void testUnknownClass() throws PCEPDeserializerException, IOException, PCEPDocumentedException {

		final PCEPObject obj = PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPObject1UnknownClass.bin")).get(
				0);

//		assertTrue(obj instanceof UnknownObject);
//		assertEquals(((UnknownObject) obj).getError(), PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
	}

//	@Test
//	public void testUnknownType() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
//		final PCEPObject obj = PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPObject2UnknownType.bin")).get(0);
//
//		assertTrue(obj instanceof UnknownObject);
//		assertEquals(((UnknownObject) obj).getError(), PCEPErrors.UNRECOGNIZED_OBJ_TYPE);
//	}
//
//	@Test
//	public void testCloseObjSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
//		serDeserTest("src/test/resources/PCEPCloseObject1.bin", new PCEPCloseObject(Reason.TOO_MANY_UNKNOWN_MSG));
//	}

	@Test
	@Ignore
	// FIXME BUG-89
	public void testLoadBalancingObjSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPLoadBalancingObject1.bin", new PCEPLoadBalancingObject(0xF1, new Bandwidth(new byte[] {
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }), true));
	}

	@Test
	public void testLspObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPLspObject1NoTlvsUpperBounds.bin", new PCEPLspObject(0xFFFFF, true, false, true, false, null));
	}

	@Test
	public void testERObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PCEPExplicitRouteObject1PackOfSubobjects.bin");
		
		MockitoAnnotations.initMocks(this);
		PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(registry);
		doReturn(parser).when(registry).getObjectParser(PCEPExplicitRouteObjectParser.TYPE, PCEPExplicitRouteObjectParser.CLASS);
		doReturn(new AsNumberSubobjectParser()).when(registry).getSubobjectParser(AsNumberSubobjectParser.TYPE);
		ObjectHeader h = new ObjectHeader() {
			
			@Override
			public Class<? extends DataContainer> getImplementedInterface() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Boolean isProcessingRule() {
				return false;
			}
			
			@Override
			public Boolean isIgnore() {
				return false;
			}
		};

		final ExplicitRouteSubobject specObj = (ExplicitRouteSubobject) registry.getObjectParser(PCEPExplicitRouteObjectParser.TYPE, PCEPExplicitRouteObjectParser.CLASS).parseObject(h, bytesFromFile);

		System.out.println(specObj.toString());

		//final byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObj));
		//assertArrayEquals(bytesFromFile, bytesActual);
	}

	@Test
	public void testIRObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PCEPIncludeRouteObject1PackOfSubobjects.bin");

		final PCEPIncludeRouteObject specObj = (PCEPIncludeRouteObject) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);

		assertEquals(8, specObj.getSubobjects().size());

		final byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObj));
		assertArrayEquals(bytesFromFile, bytesActual);
	}

	@Test
	public void tesRRObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PCEPReportedRouteObject1PackOfSubobjects.bin");

		final PCEPReportedRouteObject specObj = (PCEPReportedRouteObject) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);

		assertEquals(6, specObj.getSubobjects().size());

		final byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) specObj));
		assertArrayEquals(bytesFromFile, bytesActual);
	}

	/**
	 * Test for upper/lower bounds (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPBandwidthObject2UpperBounds.bin<br/>
	 * - PCEPBandwidthObject1LowerBounds.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	@Ignore
	// FIXME BUG-89
	public void testBandwidthObjectBounds() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPBandwidthObject1LowerBounds.bin",
				new PCEPRequestedPathBandwidthObject(new Bandwidth(new byte[] { 0, 0, 0, 0 }), true, true));
	}

	/**
	 * Test for upper/lower bounds of IPv4 EndPoints (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPEndPointsObject1IPv4.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testEndPointsObjectSerDeserIPv4() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] srcIPBytes = { (byte) 0xA2, (byte) 0xF5, (byte) 0x11, (byte) 0x0E };
		final byte[] destIPBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		serDeserTest("src/test/resources/PCEPEndPointsObject1IPv4.bin",
				new PCEPEndPointsObject<IPv4Address>(new IPv4Address(srcIPBytes), new IPv4Address(destIPBytes)));
	}

	/**
	 * Test for upper/lower bounds of IPv6 EndPoints (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPEndPointsObject2IPv6.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testEndPointsObjectSerDeserIPv6() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] destIPBytes = { (byte) 0x00, (byte) 0x02, (byte) 0x5D, (byte) 0xD2, (byte) 0xFF, (byte) 0xEC, (byte) 0xA1,
				(byte) 0xB6, (byte) 0x58, (byte) 0x1E, (byte) 0x9F, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, };
		final byte[] srcIPBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		serDeserTest("src/test/resources/PCEPEndPointsObject2IPv6.bin",
				new PCEPEndPointsObject<IPv6Address>(new IPv6Address(srcIPBytes), new IPv6Address(destIPBytes)));
	}

	/**
	 * Test of Serialization/Deserialization of PCEPErrorObjectParser.<br/>
	 * <br/>
	 * Used resources:<br/>
	 * - PCEPErrorObject1.bin<br/>
	 * - PCEPErrorObject3.bin<br/>
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testErrorObjectSerDeserWithTlv() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPErrorObject1.bin", new PCEPErrorObject(PCEPErrors.NON_OR_INVALID_OPEN_MSG));
		serDeserTest("src/test/resources/PCEPErrorObject3.bin", new PCEPErrorObject(PCEPErrors.CAPABILITY_NOT_SUPPORTED));
	}

	/**
	 * Test of validity of PCEPErrorObjectParser. Expect throwed NoSuchElementException.<br/>
	 * <br/>
	 * Used resources:<br/>
	 * - PCEPErrorObject2Invalid.bin<br/>
	 * 
	 * @throws NoSuchElementException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test(expected = PCEPDeserializerException.class)
	public void testUnknownError() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		PCEPObjectFactory.parseObjects(ByteArray.fileToBytes("src/test/resources/PCEPErrorObject2Invalid.bin")).get(0);
	}

	/**
	 * Test for upper/lower bounds of PCEPLspaObject (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPLspaObject1LowerBounds.bin<br/>
	 * - PCEPLspaObject2UpperBounds.bin<br/>
	 * - PCEPLspaObject3RandVals.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testLspaObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPLspaObject2UpperBounds.bin",
				new PCEPLspaObject(0xFFFFFFFFL, 0xFFFFFFFFL, 0xFFFFFFFFL, (short) 0xFF, (short) 0xFF, false, true, true, true));
		serDeserTest("src/test/resources/PCEPLspaObject1LowerBounds.bin",
				new PCEPLspaObject(0x00000000L, 0x00000000L, 0x00000000L, (short) 0x00, (short) 0x00, false, false, true, true));
		serDeserTest("src/test/resources/PCEPLspaObject3RandVals.bin",
				new PCEPLspaObject(0x20A1FEE3L, 0x1A025CC7L, 0x2BB66532L, (short) 0x03, (short) 0x02, false, true, true, true));
	}

	@Test
	public void testMetricObjectSerDeserBounds() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final byte[] bytesFromFileUB = ByteArray.fileToBytes("src/test/resources/PCEPMetricObject2UpperBounds.bin");
		final byte[] bytesFromFileLB = ByteArray.fileToBytes("src/test/resources/PCEPMetricObject1LowerBounds.bin");

		final PCEPMetricObject metricObjectLB = (PCEPMetricObject) PCEPObjectFactory.parseObjects(bytesFromFileLB).get(0);
		final PCEPMetricObject metricObjectUB = (PCEPMetricObject) PCEPObjectFactory.parseObjects(bytesFromFileUB).get(0);

		assertEquals(new PCEPMetricObject(false, false, new IGPMetric(0), true, true), metricObjectLB);
		assertEquals(new PCEPMetricObject(false, true, new TEMetric(4026531840L), true, true), metricObjectUB);

		final byte[] bytesActualLB = PCEPObjectFactory.put(Arrays.asList((PCEPObject) metricObjectLB));
		final byte[] bytesActualUB = PCEPObjectFactory.put(Arrays.asList((PCEPObject) metricObjectUB));
		assertArrayEquals(bytesFromFileLB, bytesActualLB);
		assertArrayEquals(bytesFromFileUB, bytesActualUB);
	}

	/**
	 * Standard deserialization test + specific test without tlv<br/>
	 * Used resources:<br/>
	 * - NoPathObject1WithTLV.bin<br/>
	 * - NoPathObject2WithoutTLV.bin<br/>
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testNoPathObjectDeserialization() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
//		final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>(1);
//		tlvs.add(new NoPathVectorTlv(false, false, true, false, false, false));
	//	serDeserTest("src/test/resources/NoPathObject1WithTLV.bin", new PCEPNoPathObject((short) 2, true, tlvs, false));
		serDeserTest("src/test/resources/NoPathObject2WithoutTLV.bin", new PCEPNoPathObject((short) 0x10, false, true));

	}

	/**
	 * Standard serialization test + without tlv<br/>
	 * Used resources:<br/>
	 * - NoPathObject1WithTLV.bin<br/>
	 * - NoPathObject2WithoutTLV.bin<br/>
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testNoPathObjectSerialization() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/NoPathObject2WithoutTLV.bin");
		PCEPNoPathObject noPathObject = (PCEPNoPathObject) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
		byte[] bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) noPathObject));
		assertArrayEquals(bytesFromFile, bytesActual);

		bytesFromFile = ByteArray.fileToBytes("src/test/resources/NoPathObject1WithTLV.bin");
		noPathObject = (PCEPNoPathObject) PCEPObjectFactory.parseObjects(bytesFromFile).get(0);
		bytesActual = PCEPObjectFactory.put(Arrays.asList((PCEPObject) noPathObject));
		assertArrayEquals(bytesFromFile, bytesActual);
	}

	/**
	 * Specific test with/without tlvs (Ser/Deser)<br/>
	 * Used resources:<br/>
	 * - PCEPNotificationObject1WithTlv.bin - PCEPNotificationObject2WithoutTlv.bin
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testNotifyObjectSerDeserWithTlv() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
//		FINAL LIST<PCEPTLV> TLVS = NEW ARRAYLIST<PCEPTLV>(1);
//		TLVS.ADD(NEW OVERLOADEDDURATIONTLV(0XFF0000A2));
//		SERDESERTEST("src/test/resources/PCEPNotificationObject1WithTlv.bin", new PCEPNotificationObject((short) 1, (short) 1, tlvs));
		serDeserTest("src/test/resources/PCEPNotificationObject2WithoutTlv.bin", new PCEPNotificationObject((short) 0xFF, (short) 0xFF));
	}

	/**
	 * Standard ser deser test<br/>
	 * used resources:<br/>
	 * - PCEPOpenObject1.bin
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	@Ignore
	// FIXME: temporary
	public void testOpenObjectSerDeser() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
//		final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
//		tlvs.add(new PCEStatefulCapabilityTlv(false, true, true));
//		tlvs.add(new LSPStateDBVersionTlv(0x80));
//		final byte[] valueBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 };
//		tlvs.add(new NodeIdentifierTlv(valueBytes));
//		final PCEPOpenObject specObject = new PCEPOpenObject(30, 120, 1, tlvs);
//
//		serDeserTest("src/test/resources/PCEPOpenObject1.bin", specObject);
	}

	/**
	 * Specific test for upper bounds and without tlvs<br/>
	 * Used resources:<br/>
	 * - PCEPOpenObject2UpperBoundsNoTlv.bin
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testOpenObjectBoundsWithoutTlvs() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
//		final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
//		serDeserTest("src/test/resources/PCEPOpenObject2UpperBoundsNoTlv.bin", new PCEPOpenObject(0xFF, 0xFF, 0xFF, tlvs));
		serDeserTest("src/test/resources/PCEPOpenObject2UpperBoundsNoTlv.bin", new PCEPOpenObject(0xFF, 0xFF, 0xFF, null));
	}

	/**
	 * Standard deserialization test<br/>
	 * Used resources:<br/>
	 * - PCEPRPObject1.bin
	 * 
	 * @throws PCEPDeserializerException
	 * @throws IOException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testRPObjectSerDeser() throws PCEPDeserializerException, IOException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPRPObject1.bin",
				new PCEPRequestParameterObject(true, false, true, true, false, false, false, false, (short) 5, 0xdeadbeefL, false, false));
//		serDeserTest(
//				"src/test/resources/PCEPRPObject2.bin",
//				new PCEPRequestParameterObject(true, false, false, false, true, false, true, false, true, (short) 5, 0xdeadbeefL, new ArrayList<PCEPTlv>() {
//					private static final long serialVersionUID = 1L;
//
//					{
//						this.add(new OrderTlv(0xFFFFFFFFL, 0x00000001L));
//					}
//				}, false, false));
	}

	/**
	 * Test for upper/lower bounds of PCEPSvecObject (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPSvecObject1_10ReqIDs.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testSvecObjectSerDeser() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final List<Long> requestIDs = new ArrayList<Long>(10);
		requestIDs.add(0xFFFFFFFFL);
		requestIDs.add(0x00000000L);
		requestIDs.add(0x01234567L);
		requestIDs.add(0x89ABCDEFL);
		requestIDs.add(0xFEDCBA98L);
		requestIDs.add(0x76543210L);
		requestIDs.add(0x15825266L);
		requestIDs.add(0x48120BBEL);
		requestIDs.add(0x25FB7E52L);
		requestIDs.add(0xB2F2546BL);

		serDeserTest("src/test/resources/PCEPSvecObject1_10ReqIDs.bin",
				new PCEPSvecObject(true, false, true, false, true, requestIDs, true));
	}

	/**
	 * Test for lowest bounds of PCEPSvecObject (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPSvecObject2.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testSvecObjectSerDeserNoReqIDs() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final List<Long> requestIDs = new ArrayList<Long>();
		requestIDs.add(0xFFL);
		serDeserTest("src/test/resources/PCEPSvecObject2.bin", new PCEPSvecObject(false, false, false, false, false, requestIDs, false));
	}

	@Test
	public void testClassTypeObject() throws PCEPDeserializerException, PCEPDocumentedException {
		final PCEPClassTypeObject ct = new PCEPClassTypeObject((short) 4);
//		final PCEPClassTypeObjectParser parser = new PCEPClassTypeObjectParser();
//		final byte[] bytes = parser.put(ct);
//		assertEquals(ct, parser.parse(bytes, true, false));
	}

	/**
	 * Test PCEPExcludeRouteObjectObject (Serialization/Deserialization)<br/>
	 * Used resources:<br/>
	 * - PCEPExcludeRouteObject.1.bin<br/>
	 * 
	 * @throws IOException
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */
	@Test
	public void testExcludeRouteObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		final List<ExcludeRouteSubobject> xroSubobjects = new ArrayList<ExcludeRouteSubobject>();
		xroSubobjects.add(new XROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 192, (byte) 168,
				(byte) 100, (byte) 100 }), 16), true, XROSubobjectAttribute.NODE));
		xroSubobjects.add(new XROAsNumberSubobject(new AsNumber(0x1234L), false));

	}

	@Test
	public void tesObjectiveFunctionObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPObjectiveFunctionObject.1.bin", new PCEPObjectiveFunctionObject(PCEPOFCodes.MBC, true, false));
	}

	@Test
	public void tesGlobalConstraintsObject() throws IOException, PCEPDeserializerException, PCEPDocumentedException {
		serDeserTest("src/test/resources/PCEPGlobalConstraintsObject.1.bin",
				new PCEPGlobalConstraintsObject((short) 1, (short) 0, (short) 100, (short) 0xFF, true, false));
	}

	// FIXME: add at least one test with true value
	@Test
	public void openObjectWithTlv() throws PCEPDeserializerException, PCEPDocumentedException {
//		this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, false, false));
//		this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, false, true));
//		this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, true, false));
//		this.testOpenObjectWithSpecTlv(new PCEStatefulCapabilityTlv(false, true, true));
	}

//	private void testOpenObjectWithSpecTlv(final PCEPTlv tlv) throws PCEPDeserializerException, PCEPDocumentedException {
//		final List<PCEPObject> objs = new ArrayList<PCEPObject>();
//		final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
//		tlvs.add(tlv);
//		final PCEPOpenObject oo = new PCEPOpenObject(30, 120, 0, tlvs);
//		objs.add(oo);
//		final byte[] bytes = PCEPObjectFactory.put(objs);
//		final PCEPObject obj = PCEPObjectFactory.parseObjects(bytes).get(0);
//		assertEquals(oo, obj);
//	}

	@Test
	public void testErrorsMapping() {
		final PCEPErrorObjectParser.PCEPErrorsMaping mapper = PCEPErrorObjectParser.PCEPErrorsMaping.getInstance();

		for (final PCEPErrors error : PCEPErrors.values()) {
			final PCEPErrorIdentifier errorId = mapper.getFromErrorsEnum(error);
			assertEquals(error, mapper.getFromErrorIdentifier(errorId));
		}
	}

	@Test
	public void testOFCodesMapping() {
		final PCEPOFCodesMapping mapper = PCEPOFCodesMapping.getInstance();

		for (final PCEPOFCodes ofCode : PCEPOFCodes.values()) {
			final int ofCodeId = mapper.getFromOFCodesEnum(ofCode);
			assertEquals(ofCode, mapper.getFromCodeIdentifier(ofCodeId));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends PCEPObject> void serDeserTestWithoutBin(final T object) throws PCEPDeserializerException,
			PCEPDocumentedException {
		final byte[] serBytes = PCEPObjectFactory.put(Arrays.asList((PCEPObject) object));
		final T deserObj = (T) PCEPObjectFactory.parseObjects(serBytes).get(0);

		assertEquals(object, deserObj);
	}

	@Test
	public void testSERObjects() throws PCEPDocumentedException, PCEPDeserializerException {
		final List<ExplicitRouteSubobject> eroSubobjects = new ArrayList<ExplicitRouteSubobject>();
		eroSubobjects.add(new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 192, (byte) 168, 1, 8 }), 16), false));
		eroSubobjects.add(new EROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(new byte[] { (byte) 192, (byte) 168, 2, 1,
				(byte) 192, (byte) 168, 2, 1, (byte) 192, (byte) 168, 2, 1, (byte) 192, (byte) 168, 2, 1 }), 64), false));

		serDeserTestWithoutBin(new PCEPSecondaryExplicitRouteObject(eroSubobjects, true, false));
	}

	@Test
	public void testSRRObject() throws PCEPDocumentedException, PCEPDeserializerException {
		final List<ReportedRouteSubobject> rroSubobjects = new ArrayList<ReportedRouteSubobject>();
		rroSubobjects.add(new RROIPAddressSubobject<IPv4Prefix>(new IPv4Prefix(this.ipv4addr, 16), true, false));
		rroSubobjects.add(new RROIPAddressSubobject<IPv6Prefix>(new IPv6Prefix(this.ipv6addr, 64), false, true));

		serDeserTestWithoutBin(new PCEPSecondaryRecordRouteObject(rroSubobjects, true, false));
	}

	@Test
	public void testP2MPEndpointsObjects() throws PCEPDeserializerException, PCEPDocumentedException {
		serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv4Address>(2, this.ipv4addr, Arrays.asList(this.ipv4addr, this.ipv4addr,
				this.ipv4addr), true, false));
		serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv4Address>(1, this.ipv4addr, Arrays.asList(this.ipv4addr), true, false));
		serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv6Address>(2, this.ipv6addr, Arrays.asList(this.ipv6addr, this.ipv6addr,
				this.ipv6addr), true, false));
		serDeserTestWithoutBin(new PCEPP2MPEndPointsObject<IPv6Address>(1, this.ipv6addr, Arrays.asList(this.ipv6addr), true, false));
	}

	@Test
	public void testUnreachedDestinationObjects() throws PCEPDeserializerException, PCEPDocumentedException {
		serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv4Address>(Arrays.asList(this.ipv4addr, this.ipv4addr, this.ipv4addr), true, false));
		serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv4Address>(Arrays.asList(this.ipv4addr), true, false));
		serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv6Address>(Arrays.asList(this.ipv6addr, this.ipv6addr, this.ipv6addr), true, false));
		serDeserTestWithoutBin(new PCEPUnreachedDestinationObject<IPv6Address>(Arrays.asList(this.ipv6addr), true, false));
	}
}
