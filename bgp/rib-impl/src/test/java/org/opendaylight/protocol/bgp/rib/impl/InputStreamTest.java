/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.PipedInputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPMessageHeader;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPInputStream;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;

public class InputStreamTest {

	@Mock
	PipedInputStream pis;

	@Mock
	BGPMessageFactory mf;

	@Mock
	BGPMessageHeader h;

	BGPInputStream is;

	@Before
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
		this.is = (BGPInputStream) BGPInputStream.FACTORY.getProtocolInputStream(this.pis, this.mf);
	}

	@Test
	public void testHeaderNotAvailable() throws IOException {
		doReturn(BGPMessageHeader.COMMON_HEADER_LENGTH - 4).when(this.pis).available();
		doReturn(-1).when(this.pis).read((byte[]) any());
		assertFalse(this.is.isMessageAvailable());
		assertFalse(this.is.header.isParsed());
	}

	@Test
	public void testHeaderAvailable() throws IOException {
		doReturn(BGPMessageHeader.COMMON_HEADER_LENGTH).when(this.pis).available();
		doReturn(5).when(this.pis).read((byte[]) any());
		assertTrue(this.is.isMessageAvailable());
		assertTrue(this.is.header.isParsed());
	}

	@Test
	public void testGetMessage() throws IOException, DeserializerException, DocumentedException {
		doReturn(BGPMessageHeader.COMMON_HEADER_LENGTH).when(this.pis).available();
		doReturn(5).when(this.pis).read((byte[]) any());
		doReturn("").when(this.h).toString();
		this.is.header = this.h;
		doNothing().when(this.h).setParsed();
		doReturn(true).when(this.h).isParsed();
		doReturn(100).when(this.h).getLength();
		doReturn(mock(BGPMessage.class)).when(this.mf).parse((byte[]) any(), any(BGPMessageHeader.class));
		assertTrue(this.is.getMessage() instanceof BGPMessage);
	}
}
