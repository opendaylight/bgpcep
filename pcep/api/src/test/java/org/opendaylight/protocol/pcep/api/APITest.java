/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.subobject.EROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;

/**
 *
 */
public class APITest {

	@Test
	public void testDeserializerException() {
		final PCEPDeserializerException e = new PCEPDeserializerException("Some error message.");
		assertEquals("Some error message.", e.getErrorMessage());

		final PCEPDeserializerException e1 = new PCEPDeserializerException(new IllegalArgumentException(), "Some error message.");
		assertEquals("Some error message.", e1.getErrorMessage());
		assertTrue(e1.getCause() instanceof IllegalArgumentException);
	}

	@Test
	public void testDocumentedException() throws PCEPDocumentedException {
		final PCEPDocumentedException de = new PCEPDocumentedException("", PCEPErrors.C_BIT_SET);
		assertEquals(PCEPErrors.C_BIT_SET, de.getError());
	}

	@Test
	public void testPCEPMessage() {
		final List<PCEPObject> objs = new ArrayList<PCEPObject>();
		objs.add(new PCEPErrorObject(PCEPErrors.ATTEMPT_2ND_SESSION));
		final PCEPMessage msg1 = new PCEPMessage(objs) {
			private static final long serialVersionUID = 1L;
		};
		final PCEPMessage msg2 = new PCEPMessage(objs) {
			private static final long serialVersionUID = 1L;
		};

		assertNotSame(msg1, msg2); //not same because they are anonymous classes
		assertEquals(msg1.hashCode(), msg2.hashCode());
		assertEquals(msg1.toString(), msg2.toString());
	}

	@Test
	public void testPCEPObject() {
		final PCEPObject obj1 = new PCEPObject(true, false) {
		};
		final PCEPObject obj2 = new PCEPErrorObject(PCEPErrors.CANNOT_PROCESS_STATE_REPORT);
		final PCEPObject obj4 = new PCEPObject(true, false) {
		};

		assertNotSame(obj1, obj2);
		assertNotSame(obj1, obj4);
		assertEquals(obj1.hashCode(), obj4.hashCode());
		assertEquals(obj1.toString(), obj4.toString());

	}

	@Test
	public void testSubobject() {
		final ExplicitRouteSubobject sub1 = new EROAsNumberSubobject(new ASNumber(100), true);
		final ExplicitRouteSubobject sub2 = new ExplicitRouteSubobject(false) {
		};
		final ExplicitRouteSubobject sub3 = new ExplicitRouteSubobject(false) {
		};
		final ExplicitRouteSubobject sub4 = new EROAsNumberSubobject(new ASNumber(100), true);

		assertNotSame(sub1, sub2);
		assertNotSame(sub2, sub3);
		assertEquals(sub1, sub4);
		assertNotSame(sub2, sub3);
		assertEquals(sub1.hashCode(), sub4.hashCode());
		assertEquals(sub2.toString(), sub3.toString());
	}
}
