/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.pcep.message.PCEPKeepAliveMessage;

public class PCEPTestingToolTest {

	@Test
	public void testSimpleSessionListener() {
		final SimpleSessionListener ssl = new SimpleSessionListener();
		assertEquals(0, ssl.messages.size());
		ssl.onMessage(null, new PCEPKeepAliveMessage());
		assertEquals(1, ssl.messages.size());
		assertTrue(ssl.messages.get(0) instanceof PCEPKeepAliveMessage);
		assertFalse(ssl.up);
		ssl.onSessionUp(null);
		assertTrue(ssl.up);
		ssl.onSessionDown(null, null);
		assertFalse(ssl.up);
	}

	@Test
	public void testSessionListenerFactory() {
		assertTrue(new SessionListenerFactory().getSessionListener() instanceof SimpleSessionListener);
	}
}
