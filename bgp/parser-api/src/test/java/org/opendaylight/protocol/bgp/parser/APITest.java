/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class APITest {

	@Test
	public void testDocumentedException() {
		final BGPDocumentedException de = new BGPDocumentedException("Some message", BGPError.BAD_BGP_ID);
		assertEquals("Some message", de.getMessage());
		assertEquals(BGPError.BAD_BGP_ID, de.getError());
		assertNull(de.getData());
	}

	@Test
	public void testParsingException() {
		final BGPParsingException de = new BGPParsingException("Some message");
		assertEquals("Some message", de.getMessage());
	}

	@Test
	public void testBGPError() {
		assertEquals(BGPError.BAD_MSG_TYPE, BGPError.forValue(1, 3));
	}

	@Test
	public void testTerminationReason() {
		assertEquals(BGPError.BAD_PEER_AS.toString(), new BGPTerminationReason(BGPError.BAD_PEER_AS).getErrorMessage());
	}
}
