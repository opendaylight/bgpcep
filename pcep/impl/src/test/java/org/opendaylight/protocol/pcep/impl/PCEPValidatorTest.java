/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.concepts.IGPMetric;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.TEMetric;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPOFCodes;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.concepts.LSPSymbolicName;
import org.opendaylight.protocol.pcep.concepts.UnnumberedInterfaceIdentifier;
import org.opendaylight.protocol.pcep.impl.message.PCEPRawMessage;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.message.PCCreateMessage;
import org.opendaylight.protocol.pcep.message.PCEPCloseMessage;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPKeepAliveMessage;
import org.opendaylight.protocol.pcep.message.PCEPNotificationMessage;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.message.PCEPReplyMessage;
import org.opendaylight.protocol.pcep.message.PCEPReportMessage;
import org.opendaylight.protocol.pcep.message.PCEPRequestMessage;
import org.opendaylight.protocol.pcep.message.PCEPUpdateRequestMessage;
import org.opendaylight.protocol.pcep.object.CompositeErrorObject;
import org.opendaylight.protocol.pcep.object.CompositeInstantiationObject;
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.protocol.pcep.object.CompositePathObject;
import org.opendaylight.protocol.pcep.object.CompositeReplySvecObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestSvecObject;
import org.opendaylight.protocol.pcep.object.CompositeResponseObject;
import org.opendaylight.protocol.pcep.object.CompositeRptPathObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdPathObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdateRequestObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExcludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPExistingPathBandwidthObject;
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
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPSvecObject;
import org.opendaylight.protocol.pcep.subobject.EROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.EROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.RROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;
import org.opendaylight.protocol.pcep.tlv.LSPStateDBVersionTlv;
import org.opendaylight.protocol.pcep.tlv.LSPSymbolicNameTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.Bandwidth;

public class PCEPValidatorTest {

	private static final PCEPLspaObject lspa = new PCEPLspaObject(0L, 0L, 0L, (short) 0, (short) 0, false, false, false, false);

	private final List<ExplicitRouteSubobject> eroSubobjects = asList(
			new EROAsNumberSubobject(new AsNumber(0xFFFFL), false),
			new EROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33 }), new UnnumberedInterfaceIdentifier(0x00FF00FF), false));

	private final List<ReportedRouteSubobject> rroSubobjects = asList((ReportedRouteSubobject) new RROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] {
			(byte) 0x00, (byte) 0x11, (byte) 0x22, (byte) 0x33 }), new UnnumberedInterfaceIdentifier(0x00FF00FF)));

	private final List<Long> requestIds = asList(0x000001L);

	private final IPv4Address ip4addr = new IPv4Address(new byte[] { (byte) 0xFF, 0x00, 0x00, 0x01 });

	private final PCEPSvecObject svecObj = new PCEPSvecObject(true, true, true, false, false, PCEPValidatorTest.this.requestIds, true);

	private final PCEPRequestParameterObject requestParameter = new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, true, false);

	// private final PCEPEndPointsObject<IPv4Address> endPoints = new
	// PCEPEndPointsObject<IPv4Address>(this.ip4addr, this.ip4addr);

	private final PCEPEndPointsObject<IPv4Address> endPoints = new PCEPEndPointsObject<IPv4Address>(this.ip4addr, this.ip4addr);

	private static final RawPCEPMessageFactory msgFactory = new RawPCEPMessageFactory();

	// private final PCEPClassTypeObject classType = new
	// PCEPClassTypeObject((short) 7);
	// private final PCEPClassTypeObjectProvider classTypeProvider = new
	// PCEPClassTypeObjectProvider((short) 7, true);

	private static List<PCEPMessage> deserMsg(final String srcFile) throws IOException, DeserializerException, DocumentedException,
			PCEPDeserializerException {
		final byte[] bytesFromFile = ByteArray.fileToBytes(srcFile);
		final PCEPRawMessage rawMessage = (PCEPRawMessage) msgFactory.parse(bytesFromFile).get(0);

		return PCEPMessageValidator.getValidator(rawMessage.getMsgType()).validate(rawMessage.getAllObjects());
	}

	@Test
	public void testOpenMessageValidationFromBin() throws IOException, DeserializerException, DocumentedException,
			PCEPDeserializerException {
		assertEquals(
				deserMsg("src/test/resources/PCEPOpenMessage1.bin"),
				asList(new PCEPOpenMessage(new PCEPOpenObject(30, 120, 1, asList(new PCEStatefulCapabilityTlv(false, true, true),
						new LSPStateDBVersionTlv(0x80))))));

		assertEquals(deserMsg("src/test/resources/Open.1.bin"), asList(new PCEPOpenMessage(new PCEPOpenObject(1, 4, 1))));

		assertEquals(
				deserMsg("src/test/resources/Open.3.bin"),
				asList(new PCEPOpenMessage(new PCEPOpenObject(1, 4, 1, asList(new PCEStatefulCapabilityTlv(false, true, true),
						new LSPStateDBVersionTlv(53))))));
	}

	@Test
	public void testKeepAliveMessageValidationFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {
		assertEquals(deserMsg("src/test/resources/PCEPKeepAliveMessage1.bin"), asList(new PCEPKeepAliveMessage()));
		assertEquals(deserMsg("src/test/resources/Keepalive.1.bin"), asList(new PCEPKeepAliveMessage()));
	}

	@Test
	public void testCloseMsg() throws PCEPDeserializerException, IOException, PCEPDocumentedException, DeserializerException,
			DocumentedException {
		assertEquals(deserMsg("src/test/resources/PCEPCloseMessage1.bin"),
				asList(new PCEPCloseMessage(new PCEPCloseObject(Reason.TOO_MANY_UNKNOWN_MSG))));
		assertEquals(deserMsg("src/test/resources/Close.1.bin"), asList(new PCEPCloseMessage(new PCEPCloseObject(Reason.UNKNOWN))));
	}

	@Test
	public void testRequestMessageValidationFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {
		List<CompositeRequestObject> requests = new ArrayList<CompositeRequestObject>();
		final byte[] ipAdress = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
		requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 5, 0xDEADBEEFL, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress), new IPv4Address(ipAdress))));
		PCEPRequestMessage specMessage = new PCEPRequestMessage(requests);
		List<PCEPMessage> deserMsgs = deserMsg("src/test/resources/PCEPRequestMessage1.bin");
		final List<PCEPMessage> specMessages = new ArrayList<PCEPMessage>();
		specMessages.add(specMessage);

		assertEquals(deserMsgs, specMessages);

		requests = new ArrayList<CompositeRequestObject>();
		final byte[] ipAdress2 = { (byte) 0x7F, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress2), new IPv4Address(ipAdress2))));
		specMessage = new PCEPRequestMessage(requests);
		deserMsgs = deserMsg("src/test/resources/PCReq.1.bin");
		specMessages.clear();
		specMessages.add(specMessage);
		assertEquals(deserMsgs, specMessages);

		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, new PCEPLoadBalancingObject(3, new Bandwidth(ByteArray.floatToBytes((float) 1024.75)), false)));
		specMessage = new PCEPRequestMessage(requests);
		deserMsgs = deserMsg("src/test/resources/PCReq.2.bin");
		specMessages.clear();
		specMessages.add(specMessage);
		// FIXME BUG-89
		// assertEquals(deserMsgs, specMessages);

		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, new PCEPLspObject(1, false, false, true, false), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(1000)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new IGPMetric(53L), false, false));
			}
		}, new PCEPReportedRouteObject(this.rroSubobjects, false), new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPIncludeRouteObject(this.eroSubobjects, false, false), new PCEPLoadBalancingObject(5, new Bandwidth(ByteArray.floatToBytes(3)), false)));

		List<CompositeRequestSvecObject> svecList = new ArrayList<CompositeRequestSvecObject>();
		svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(true, false, false, false, false, this.requestIds, false)));

		specMessage = new PCEPRequestMessage(svecList, requests);
		deserMsgs = deserMsg("src/test/resources/PCReq.3.bin");
		specMessages.clear();
		specMessages.add(specMessage);
		// FIXME BUG-89
		// assertEquals(deserMsgs, specMessages);

		specMessages.clear();
		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, null));
		specMessages.add(new PCEPRequestMessage(requests));

		final byte[] ipAdress3 = { (byte) 0x7F, (byte) 0x00, (byte) 0x30, (byte) 0x01 };
		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(false, false, false, false, false, false, false, false, (short) 4, 1, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress3), new IPv4Address(ipAdress2)), null, null, null, null, null, null, null, null, null));
		specMessages.add(new PCEPRequestMessage(requests));

		final byte[] ipAdress4 = { (byte) 0x7F, (byte) 0x30, (byte) 0x00, (byte) 0x01 };
		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress2), new IPv4Address(ipAdress4)), null, null, null, null, null, null, null, null, null));
		specMessages.add(new PCEPRequestMessage(requests));

		final byte[] ipAdress5 = { (byte) 0x7F, (byte) 0xd0, (byte) 0x00, (byte) 0x01 };
		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 1, true, false), new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress5), new IPv4Address(ipAdress5)), null, null, null, null, null, null, null, null, null));

		specMessages.add(new PCEPRequestMessage(requests));
		deserMsgs = deserMsg("src/test/resources/PCReq.4.bin");
		assertEquals(deserMsgs, specMessages);

		specMessages.clear();
		svecList = new ArrayList<CompositeRequestSvecObject>();
		svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(true, false, false, false, false, this.requestIds, false)));
		svecList.add(new CompositeRequestSvecObject(new PCEPSvecObject(false, true, true, false, false, this.requestIds, false), new PCEPObjectiveFunctionObject(PCEPOFCodes.MCC, true, false), new PCEPGlobalConstraintsObject((short) 0x55, (short) 1, (short) 100, (short) 0x26, true, false), new PCEPExcludeRouteObject(new ArrayList<ExcludeRouteSubobject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new XROAsNumberSubobject(new AsNumber((long) 0x12), true));
			}
		}, true, true, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new TEMetric(123456L), true, false));
			}
		}));

		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress2), new IPv4Address(ipAdress2)), null, null, PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(1000)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new IGPMetric(53L), false, false));
				this.add(new PCEPMetricObject(true, true, new IGPMetric(5335L), false, false));
				this.add(new PCEPMetricObject(true, true, new IGPMetric(128256), false, false));
			}
		}, new PCEPReportedRouteObject(this.rroSubobjects, false), new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPIncludeRouteObject(this.eroSubobjects, false, false), new PCEPLoadBalancingObject(5, new Bandwidth(ByteArray.floatToBytes(3)), false)));

		final byte[] ipAdress6 = { (byte) 0x7F, (byte) 0xF0, (byte) 0x00, (byte) 0x01 };
		specMessages.add(new PCEPRequestMessage(svecList, requests));

		requests = new ArrayList<CompositeRequestObject>();
		requests.add(new CompositeRequestObject(this.requestParameter, new PCEPEndPointsObject<IPv4Address>(new IPv4Address(ipAdress6), new IPv4Address(ipAdress6)), null, null, PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(1000)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new IGPMetric(53L), false, false));
			}
		}, new PCEPReportedRouteObject(this.rroSubobjects, false), new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPIncludeRouteObject(this.eroSubobjects, false, false), new PCEPLoadBalancingObject(5, new Bandwidth(ByteArray.floatToBytes(3f)), false)));
		deserMsgs = deserMsg("src/test/resources/PCReq.5.bin");
		specMessages.add(new PCEPRequestMessage(svecList, requests));
		// FIXME BUG-89
		// assertEquals(deserMsgs, specMessages);

		// FIXME: need construct with invalid processed parameter
		// assertEquals(deserMsg("src/test/resources/PCReq.6.invalid.bin"),
		// asList(
		// new PCEPErrorMessage(new CompositeErrorObject(new
		// PCEPRequestParameterObject(true, false, false, false, false, false,
		// false, false, (short) 3,
		// 1L, false, false), new PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))),
		// new PCEPRequestMessage(asList(new
		// CompositeRequestObject(this.requestParameter, new
		// PCEPEndPointsObject<IPv4Address>(IPv4Address
		// .getNetworkAddressFactory().getNetworkAddressForBytes(new byte[] {
		// 127, 0, 0, 1 }), IPv4Address.getNetworkAddressFactory()
		// .getNetworkAddressForBytes(new byte[] { 127, 0, 0, 1 })), null, null,
		// null, null, null, null, null, null, new PCEPLoadBalancingObject(
		// 3, new Bandwidth(1024.75), false))))));

	}

	@Test
	public void testRequestMessageValidationFromRawMsg() throws PCEPDeserializerException {
		List<PCEPObject> objs = new ArrayList<PCEPObject>();
		List<PCEPMessage> msgs;
		PCEPRequestParameterObject tmpRP;

		// test unrecognized object in svec list
		objs.add(this.svecObj);
		objs.add(new UnknownObject(true, false, PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
		objs.add(new PCEPSvecObject(true, true, true, false, false, PCEPValidatorTest.this.requestIds, true));

		msgs = PCEPMessageValidator.getValidator(PCEPMessageType.REQUEST).validate(objs);

		assertEquals(msgs.get(0), new PCEPErrorMessage(new ArrayList<PCEPErrorObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
			}
		}));

		// test with request p flag not set and ignoracion of more than one
		// end-points objects
		objs = new ArrayList<PCEPObject>();
		objs.add(this.svecObj);
		objs.add(this.svecObj);
		tmpRP = new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false);
		objs.add(tmpRP);
		objs.add(this.endPoints);

		objs.add(this.requestParameter);
		objs.add(this.endPoints);
		objs.add(this.endPoints);
		// FIXME:mv use object constructor with set processed flag
		// objs.add(this.classTypeProvider);
		// objs.add(this.requestParameter);
		// objs.add(this.endPointsProvider);
		// objs.add(new PCEPClassTypeObjectProvider((short) 7, false));

		msgs = PCEPMessageValidator.getValidator(PCEPMessageType.REQUEST).validate(objs);
		// FIXME:mv use object constructor with set processed flag
		// assertEquals(msgs.get(0), new PCEPErrorMessage(new
		// CompositeErrorObject(tmpRP, new
		// PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
		// assertEquals(
		// msgs.get(1),
		// new PCEPRequestMessage(asList(new
		// CompositeRequestSvecObject(this.svecObj), new
		// CompositeRequestSvecObject(this.svecObj)), Util
		// .asList(new CompositeRequestObject(this.requestParameter,
		// this.endPoints, this.classType, null, null, null, null, null, null,
		// null,
		// null))));
		// assertEquals(msgs.get(2), new PCEPErrorMessage(new
		// CompositeErrorObject(tmpRP, new
		// PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
	}

	@Test
	public void testReplyMessageValidatorFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {

		List<PCEPReplyMessage> specMessages = new ArrayList<PCEPReplyMessage>();
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 5, 0xDEADBEEFL, true, true)))));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, true, true, false, false, false, false, false, (short) 7, 0x12345678L, false, false)))));
		assertEquals(deserMsg("src/test/resources/PCEPReplyMessage1.bin"), specMessages);

		specMessages = new ArrayList<PCEPReplyMessage>();
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false)))));
		assertEquals(deserMsg("src/test/resources/PCRep.1.bin"), specMessages);

		specMessages = new ArrayList<PCEPReplyMessage>();
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false)))));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(false, false, false, false, false, false, false, false, (short) 5, 2, false, false), new PCEPNoPathObject((short) 0, false, false), null, null, null, null, null, null))));
		assertEquals(deserMsg("src/test/resources/PCRep.2.bin"), specMessages);

		specMessages = new ArrayList<PCEPReplyMessage>();
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false), new PCEPNoPathObject((short) 1, true, false), new PCEPLspObject(1, true, true, false, true), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new IGPMetric(234), false, false));
			}
		}, new PCEPIncludeRouteObject(this.eroSubobjects, false, false), new ArrayList<CompositePathObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositePathObject(new PCEPExplicitRouteObject(PCEPValidatorTest.this.eroSubobjects, false), lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new IGPMetric(234L), false, false));
					}
				}, new PCEPIncludeRouteObject(PCEPValidatorTest.this.eroSubobjects, false, false)));
			}
		}))));
		// FIXME BUG-89
		// assertEquals(deserMsg("src/test/resources/PCRep.3.bin"), specMessages);

		specMessages = new ArrayList<PCEPReplyMessage>();
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 7, 1, false, false)))));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 2, false, false)))));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 2, 4, false, false)))));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(false, false, false, false, false, false, false, false, (short) 3, 4, false, false)))));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(false, false, false, false, false, false, false, false, (short) 6, 5, false, false)))));
		assertEquals(deserMsg("src/test/resources/PCRep.4.bin"), specMessages);

		specMessages = new ArrayList<PCEPReplyMessage>();
		final List<Long> requestIDs = new ArrayList<Long>();
		requestIDs.add(0x25069045L);

		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();
		metrics.add(new PCEPMetricObject(true, true, new IGPMetric(234L), true, false));

		final List<CompositeReplySvecObject> svecList = new ArrayList<CompositeReplySvecObject>();
		svecList.add(new CompositeReplySvecObject(new PCEPSvecObject(true, true, true, false, false, requestIDs, true), new PCEPObjectiveFunctionObject(PCEPOFCodes.MCC, true, false), metrics));

		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false), new PCEPNoPathObject((short) 1, true, false), null, PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new IGPMetric(234), false, false));
			}
		}, new PCEPIncludeRouteObject(this.eroSubobjects, false, false), new ArrayList<CompositePathObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositePathObject(new PCEPExplicitRouteObject(PCEPValidatorTest.this.eroSubobjects, false), lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new IGPMetric(234L), false, false));
						this.add(new PCEPMetricObject(true, true, new IGPMetric(5355L), false, false));
						this.add(new PCEPMetricObject(true, true, new IGPMetric(5353L), false, false));
					}
				}, new PCEPIncludeRouteObject(PCEPValidatorTest.this.eroSubobjects, false, false)));
			}
		})), svecList));
		specMessages.add(new PCEPReplyMessage(asList(new CompositeResponseObject(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false), new PCEPNoPathObject((short) 1, true, false), null, PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, true, new IGPMetric(234), false, false));
			}
		}, new PCEPIncludeRouteObject(this.eroSubobjects, false, false), new ArrayList<CompositePathObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositePathObject(new PCEPExplicitRouteObject(PCEPValidatorTest.this.eroSubobjects, false), lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new IGPMetric(234L), false, false));
					}
				}, new PCEPIncludeRouteObject(PCEPValidatorTest.this.eroSubobjects, false, false)));
				this.add(new CompositePathObject(new PCEPExplicitRouteObject(PCEPValidatorTest.this.eroSubobjects, false), lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(500)), false, false), new ArrayList<PCEPMetricObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPMetricObject(true, true, new IGPMetric(234L), false, false));
					}
				}, new PCEPIncludeRouteObject(PCEPValidatorTest.this.eroSubobjects, false, false)));
			}
		})), svecList));
		// FIXME : BUG-89
		// assertEquals(deserMsg("src/test/resources/PCRep.5.bin"), specMessages);
	}

	@Test
	public void testUpdMessageValidatorFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {
		List<PCEPMessage> specMessages = new ArrayList<PCEPMessage>();

		List<CompositeUpdateRequestObject> requests = new ArrayList<CompositeUpdateRequestObject>();
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));

		specMessages.add(new PCEPUpdateRequestMessage(requests));
		assertEquals(deserMsg("src/test/resources/PCUpd.1.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		List<CompositeUpdPathObject> paths = new ArrayList<CompositeUpdPathObject>();
		paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, null, null));
		requests = new ArrayList<CompositeUpdateRequestObject>();
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
		specMessages.add(new PCEPUpdateRequestMessage(requests));
		assertEquals(deserMsg("src/test/resources/PCUpd.2.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		paths = new ArrayList<CompositeUpdPathObject>();
		paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		requests = new ArrayList<CompositeUpdateRequestObject>();
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
		specMessages.add(new PCEPUpdateRequestMessage(requests));
		// FIXME BUG-89
		// assertEquals(deserMsg("src/test/resources/PCUpd.3.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		requests = new ArrayList<CompositeUpdateRequestObject>();
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
		specMessages.add(new PCEPUpdateRequestMessage(requests));
		assertEquals(deserMsg("src/test/resources/PCUpd.4.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		requests = new ArrayList<CompositeUpdateRequestObject>();
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true)));
		paths = new ArrayList<CompositeUpdPathObject>();
		paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
		paths = new ArrayList<CompositeUpdPathObject>();
		paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		paths.add(new CompositeUpdPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPRequestedPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		requests.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, false, true, true), paths));
		specMessages.add(new PCEPUpdateRequestMessage(requests));
		// FIXME BUG-89
		// assertEquals(deserMsg("src/test/resources/PCUpd.5.bin"), specMessages);
	}

	@Test
	public void testRptMessageValidatorFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {
		List<PCEPMessage> specMessages = new ArrayList<PCEPMessage>();
		List<CompositeStateReportObject> reports = new ArrayList<CompositeStateReportObject>();
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
		specMessages.add(new PCEPReportMessage(reports));
		assertEquals(deserMsg("src/test/resources/PCRpt.1.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		List<CompositeRptPathObject> paths = new ArrayList<CompositeRptPathObject>();
		paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, null, null, null));
		reports = new ArrayList<CompositeStateReportObject>();
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
		specMessages.add(new PCEPReportMessage(reports));
		assertEquals(deserMsg("src/test/resources/PCRpt.2.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		paths = new ArrayList<CompositeRptPathObject>();
		paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));

		reports = new ArrayList<CompositeStateReportObject>();
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
		specMessages.add(new PCEPReportMessage(reports));
		// FIXME:
		// assertEquals(deserMsg("src/test/resources/PCRpt.3.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		reports = new ArrayList<CompositeStateReportObject>();
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true)));
		specMessages.add(new PCEPReportMessage(reports));
		assertEquals(deserMsg("src/test/resources/PCRpt.4.bin"), specMessages);

		specMessages = new ArrayList<PCEPMessage>();
		reports = new ArrayList<CompositeStateReportObject>();
		paths = new ArrayList<CompositeRptPathObject>();
		paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
		paths = new ArrayList<CompositeRptPathObject>();
		paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		paths.add(new CompositeRptPathObject(new PCEPExplicitRouteObject(this.eroSubobjects, false), PCEPValidatorTest.lspa, new PCEPExistingPathBandwidthObject(new Bandwidth(ByteArray.floatToBytes(5353)), false, false), new PCEPReportedRouteObject(this.rroSubobjects, false), new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		reports.add(new CompositeStateReportObject(new PCEPLspObject(1, true, false, true, true), paths));
		specMessages.add(new PCEPReportMessage(reports));
		// FIXME
		// assertEquals(deserMsg("src/test/resources/PCRpt.5.bin"), specMessages);
	}

	@Test
	public void testPCCreateMessage() throws DeserializerException, DocumentedException, PCEPDeserializerException {
		final List<CompositeInstantiationObject> insts = new ArrayList<CompositeInstantiationObject>();
		final List<ExplicitRouteSubobject> subs = new ArrayList<ExplicitRouteSubobject>();
		subs.add(new EROAsNumberSubobject(new AsNumber((long) 10), false));
		final List<PCEPTlv> tlvs = new ArrayList<PCEPTlv>();
		final LSPSymbolicNameTlv tlv = new LSPSymbolicNameTlv(new LSPSymbolicName(new byte[] { 5, 4 }));
		tlvs.add(tlv);
		insts.add(new CompositeInstantiationObject(new PCEPEndPointsObject<IPv4Address>(IPv4.FAMILY.addressForString("127.0.0.2"), IPv4.FAMILY.addressForString("127.0.0.1")), PCEPValidatorTest.lspa, new PCEPExplicitRouteObject(subs, true), null, new ArrayList<PCEPMetricObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
				this.add(new PCEPMetricObject(true, false, new IGPMetric(4L), false, false));
			}
		}));
		final PCCreateMessage msg = new PCCreateMessage(insts);

		final byte[] bytes = msgFactory.put(msg);

		// FIXME: need construct with invalid processed parameter
		final PCEPRawMessage rawMessage = (PCEPRawMessage) msgFactory.parse(bytes).get(0);

		assertEquals(PCEPMessageValidator.getValidator(rawMessage.getMsgType()).validate(rawMessage.getAllObjects()),
				asList((PCEPMessage) msg));
	}

	@Test
	public void testNotificationValidatorFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {
		List<CompositeNotifyObject> notifications = new ArrayList<CompositeNotifyObject>();
		List<PCEPNotificationObject> notificationsList = new ArrayList<PCEPNotificationObject>();
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		notifications.add(new CompositeNotifyObject(notificationsList));
		PCEPNotificationMessage specMessage = new PCEPNotificationMessage(notifications);
		assertEquals(deserMsg("src/test/resources/PCNtf.1.bin"), asList((PCEPMessage) specMessage));

		notifications = new ArrayList<CompositeNotifyObject>();
		notificationsList = new ArrayList<PCEPNotificationObject>();
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		List<PCEPRequestParameterObject> requestsList = new ArrayList<PCEPRequestParameterObject>();
		requestsList.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false));
		notifications.add(new CompositeNotifyObject(requestsList, notificationsList));
		specMessage = new PCEPNotificationMessage(notifications);
		assertEquals(deserMsg("src/test/resources/PCNtf.2.bin"), asList((PCEPMessage) specMessage));

		notifications = new ArrayList<CompositeNotifyObject>();
		notificationsList = new ArrayList<PCEPNotificationObject>();
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		requestsList = new ArrayList<PCEPRequestParameterObject>();
		requestsList.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 10, false, false));
		notifications.add(new CompositeNotifyObject(requestsList, notificationsList));
		specMessage = new PCEPNotificationMessage(notifications);
		assertEquals(deserMsg("src/test/resources/PCNtf.3.bin"), asList((PCEPMessage) specMessage));

		notifications = new ArrayList<CompositeNotifyObject>();
		notificationsList = new ArrayList<PCEPNotificationObject>();
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 2));
		notificationsList.add(new PCEPNotificationObject((short) 2, (short) 1));
		notificationsList.add(new PCEPNotificationObject((short) 2, (short) 2));
		notifications.add(new CompositeNotifyObject(notificationsList));
		specMessage = new PCEPNotificationMessage(notifications);
		assertEquals(deserMsg("src/test/resources/PCNtf.4.bin"), asList((PCEPMessage) specMessage));

		notifications = new ArrayList<CompositeNotifyObject>();
		notificationsList = new ArrayList<PCEPNotificationObject>();
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		requestsList = new ArrayList<PCEPRequestParameterObject>();
		requestsList.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 10, false, false));
		notifications.add(new CompositeNotifyObject(requestsList, notificationsList));
		notificationsList = new ArrayList<PCEPNotificationObject>();
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		notificationsList.add(new PCEPNotificationObject((short) 1, (short) 1));
		requestsList = new ArrayList<PCEPRequestParameterObject>();
		requestsList.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 10, false, false));
		requestsList.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 10, false, false));
		notifications.add(new CompositeNotifyObject(requestsList, notificationsList));
		specMessage = new PCEPNotificationMessage(notifications);
		assertEquals(deserMsg("src/test/resources/PCNtf.5.bin"), asList((PCEPMessage) specMessage));
	}

	@Test
	public void testErrorMessageValidatoinFromBin() throws IOException, PCEPDeserializerException, PCEPDocumentedException,
			DeserializerException, DocumentedException {
		List<PCEPErrorObject> errorsList = new ArrayList<PCEPErrorObject>();
		errorsList.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));

		PCEPErrorMessage specMessage = new PCEPErrorMessage(errorsList);
		assertEquals(deserMsg("src/test/resources/PCErr.1.bin"), asList((PCEPMessage) specMessage));

		List<PCEPRequestParameterObject> requests = new ArrayList<PCEPRequestParameterObject>();
		requests.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 3, 1, false, false));

		List<CompositeErrorObject> errors = new ArrayList<CompositeErrorObject>();
		errors.add(new CompositeErrorObject(requests, errorsList));

		specMessage = new PCEPErrorMessage(errors);
		assertEquals(deserMsg("src/test/resources/PCErr.2.bin"), asList((PCEPMessage) specMessage));

		specMessage = new PCEPErrorMessage(new PCEPOpenObject(0, 0, 0), errorsList, null);
		assertEquals(deserMsg("src/test/resources/PCErr.3.bin"), asList((PCEPMessage) specMessage));

		requests = new ArrayList<PCEPRequestParameterObject>();
		requests.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 53, false, false));

		errors = new ArrayList<CompositeErrorObject>();
		errors.add(new CompositeErrorObject(requests, errorsList));

		specMessage = new PCEPErrorMessage(errors);
		assertEquals(deserMsg("src/test/resources/PCErr.3b.bin"), asList((PCEPMessage) specMessage));

		errorsList = new ArrayList<PCEPErrorObject>();
		errorsList.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
		errorsList.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));
		errorsList.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));

		specMessage = new PCEPErrorMessage(null, errorsList, null);
		assertEquals(deserMsg("src/test/resources/PCErr.4.bin"), asList((PCEPMessage) specMessage));

		requests = new ArrayList<PCEPRequestParameterObject>();
		requests.add(new PCEPRequestParameterObject(true, false, false, false, false, false, false, false, (short) 1, 53, false, false));

		errorsList = new ArrayList<PCEPErrorObject>();
		errorsList.add(new PCEPErrorObject(PCEPErrors.UNRECOGNIZED_OBJ_CLASS));

		errors = new ArrayList<CompositeErrorObject>();
		errors.add(new CompositeErrorObject(requests, errorsList));
		errors.add(new CompositeErrorObject(requests, errorsList));

		specMessage = new PCEPErrorMessage(errors);
		assertEquals(deserMsg("src/test/resources/PCErr.5.bin"), asList((PCEPMessage) specMessage));
	}
}
