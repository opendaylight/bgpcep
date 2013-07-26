/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.rib.impl.BGPImpl.BGPListenerRegistration;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnection;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;

public class BGPImplTest {

	@Mock
	private BGPDispatcher disp;

	@Mock
	private BGPSessionProposal prop;

	@Mock
	private ProtocolMessageFactory parser;

	private BGPImpl bgp;

	@Before
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
		doReturn("").when(this.parser).toString();
		doReturn(null).when(this.disp).createClient(any(BGPConnection.class), any(ProtocolMessageFactory.class));
	}

	@Test
	public void testBgpImpl() throws IOException {
		doReturn(new BGPSessionPreferences(null, 0, null, Collections.<BGPParameter> emptyList())).when(this.prop).getProposal();
		this.bgp = new BGPImpl(this.disp, this.parser, null, this.prop, null);
		final BGPListenerRegistration reg = (BGPListenerRegistration) this.bgp.registerUpdateListener(new SimpleSessionListener());
		assertEquals(SimpleSessionListener.class, reg.getListener().getClass());
	}

	@After
	public void tearDown() {
		this.bgp.close();
	}
}
