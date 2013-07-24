/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.pcep.PCEPErrors;
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
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.protocol.pcep.object.CompositeRequestObject;
import org.opendaylight.protocol.pcep.object.CompositeResponseObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdateRequestObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;

public class MessagesTest {

	@Test
	public void errorMessageTest() {
		final List<PCEPErrorObject> errorObjs = new ArrayList<PCEPErrorObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new PCEPErrorObject(PCEPErrors.ATTEMPT_2ND_SESSION));
				this.add(new PCEPErrorObject(PCEPErrors.ATTEMPT_2ND_SESSION));
			}
		};
		final List<CompositeErrorObject> errors = new ArrayList<CompositeErrorObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeErrorObject(errorObjs));
			}
		};

		final List<?> objs = new ArrayList<Object>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeErrorObject(errorObjs));
				this.add(new PCEPErrorObject(PCEPErrors.BANDWIDTH_MISSING));
			}
		};

		final PCEPErrorMessage m = new PCEPErrorMessage(new PCEPOpenObject(10, 10, 1), errorObjs, errors);
		final PCEPErrorMessage m2 = new PCEPErrorMessage(new PCEPOpenObject(10, 10, 1), errorObjs, errors);
		final PCEPErrorMessage m3 = new PCEPErrorMessage(objs);

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getOpenObject(), m2.getOpenObject());
		assertEquals(m.getErrors(), m2.getErrors());
		assertEquals(m.getErrorObjects(), m2.getErrorObjects());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPCloseMessage(new PCEPCloseObject(Reason.EXP_DEADTIMER))));
		assertTrue(m.equals(m));
	}

	@Test(expected = IllegalArgumentException.class)
	public void closeMessageTest() {
		final PCEPCloseMessage m = new PCEPCloseMessage(new PCEPCloseObject(Reason.EXP_DEADTIMER));
		final PCEPCloseMessage m2 = new PCEPCloseMessage(new PCEPCloseObject(Reason.EXP_DEADTIMER));
		final PCEPCloseMessage m3 = new PCEPCloseMessage(new PCEPCloseObject(Reason.MALFORMED_MSG));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getCloseObject(), m2.getCloseObject());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		new PCEPCloseMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void openMessageTest() {
		final PCEPOpenMessage m = new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1));
		final PCEPOpenMessage m2 = new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1));
		final PCEPOpenMessage m3 = new PCEPOpenMessage(new PCEPOpenObject(5, 5, 1));

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getOpenObject(), m2.getOpenObject());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPCloseMessage(new PCEPCloseObject(Reason.EXP_DEADTIMER))));
		assertTrue(m.equals(m));

		new PCEPOpenMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void keepAliveMessageTest() {
		final PCEPKeepAliveMessage m = new PCEPKeepAliveMessage();
		final PCEPKeepAliveMessage m2 = new PCEPKeepAliveMessage();

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertFalse(m.equals(null));
		assertFalse(m.equals(new PCEPCloseMessage(new PCEPCloseObject(Reason.EXP_DEADTIMER))));
		assertTrue(m.equals(m));

		new PCEPOpenMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void notifyMessageTest() {
		final List<CompositeNotifyObject> notifications = new ArrayList<CompositeNotifyObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeNotifyObject(new ArrayList<PCEPNotificationObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPNotificationObject((short) 2, (short) 3));
					}
				}));
			}
		};

		final PCEPNotificationMessage m = new PCEPNotificationMessage(notifications);
		final PCEPNotificationMessage m2 = new PCEPNotificationMessage(notifications);
		final PCEPNotificationMessage m3 = new PCEPNotificationMessage(new ArrayList<CompositeNotifyObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeNotifyObject(new ArrayList<PCEPNotificationObject>() {
					private static final long serialVersionUID = 1L;

					{
						this.add(new PCEPNotificationObject((short) 2, (short) 5));
					}
				}));
			}
		});

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getNotifications(), m2.getNotifications());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		new PCEPNotificationMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void replyMessageTest() {
		final List<CompositeResponseObject> replies = new ArrayList<CompositeResponseObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeResponseObject(new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 1, 1, true,
						false)));
			}
		};

		final PCEPReplyMessage m = new PCEPReplyMessage(replies);
		final PCEPReplyMessage m2 = new PCEPReplyMessage(replies);
		final PCEPReplyMessage m3 = new PCEPReplyMessage(new ArrayList<CompositeResponseObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeResponseObject(new PCEPRequestParameterObject(true, true, true, false, true, false, false, false, (short) 2, 1, false,
						false)));
			}
		});

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getResponses(), m2.getResponses());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		new PCEPReplyMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void reportMessageTest() {
		final List<CompositeStateReportObject> reports = new ArrayList<CompositeStateReportObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeStateReportObject(new PCEPLspObject(1, true, true, true, true)));
			}
		};

		final PCEPReportMessage m = new PCEPReportMessage(reports);
		final PCEPReportMessage m2 = new PCEPReportMessage(reports);
		final PCEPReportMessage m3 = new PCEPReportMessage(new ArrayList<CompositeStateReportObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeStateReportObject(new PCEPLspObject(5, false, true, true, true)));
			}
		});

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getStateReports(), m2.getStateReports());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		new PCEPReportMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void requestMessageTest() {
		final List<CompositeRequestObject> reports = new ArrayList<CompositeRequestObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeRequestObject(
						new PCEPRequestParameterObject(true, true, true, true, true, false, false, false, (short) 5, 5, true, true),
						new PCEPEndPointsObject<IPv4Address>(
							new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 }),
							new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 }))));
			}
		};

		final PCEPRequestMessage m = new PCEPRequestMessage(reports);
		final PCEPRequestMessage m2 = new PCEPRequestMessage(reports);
		final PCEPRequestMessage m3 = new PCEPRequestMessage(new ArrayList<CompositeRequestObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeRequestObject(
						new PCEPRequestParameterObject(true, true, true, false, true, false, false, false, (short) 5, 5, true, true),
						new PCEPEndPointsObject<IPv4Address>(
							new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 2 }),
							new IPv4Address(new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 }))));
			}
		});

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getSvecObjects(), m2.getSvecObjects());
		assertEquals(m.getRequests(), m2.getRequests());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		new PCEPRequestMessage(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateRequestMessageTest() {
		final List<CompositeUpdateRequestObject> reports = new ArrayList<CompositeUpdateRequestObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeUpdateRequestObject(new PCEPLspObject(1, true, true, true, true)));
			}
		};

		final PCEPUpdateRequestMessage m = new PCEPUpdateRequestMessage(reports);
		final PCEPUpdateRequestMessage m2 = new PCEPUpdateRequestMessage(reports);
		final PCEPUpdateRequestMessage m3 = new PCEPUpdateRequestMessage(new ArrayList<CompositeUpdateRequestObject>() {
			private static final long serialVersionUID = 1L;

			{
				this.add(new CompositeUpdateRequestObject(new PCEPLspObject(5, true, true, true, true)));
			}
		});

		assertEquals(m, m2);
		assertEquals(m.toString(), m2.toString());
		assertEquals(m.hashCode(), m2.hashCode());
		assertEquals(m.getUpdateRequests(), m2.getUpdateRequests());
		assertFalse(m.equals(null));
		assertFalse(m.equals(m3));
		assertFalse(m.equals(new PCEPOpenMessage(new PCEPOpenObject(10, 10, 1))));
		assertTrue(m.equals(m));

		new PCEPUpdateRequestMessage(null);
	}
}
