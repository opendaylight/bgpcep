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
import io.netty.util.concurrent.Future;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPSession;
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

	@Mock
	private Future<BGPSession> future;

	private BGPImpl bgp;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		doReturn("").when(this.parser).toString();

		doReturn(null).when(this.future).get();
		doReturn(future).when(this.disp).createClient(any(BGPConnection.class), any(ProtocolMessageFactory.class));
	}

	@Test
	public void testBgpImpl() throws Exception {
		doReturn(new BGPSessionPreferences(null, 0, null, Collections.<BGPParameter> emptyList())).when(this.prop).getProposal();
		this.bgp = new BGPImpl(this.disp, this.parser, new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000), this.prop, null);
		final BGPListenerRegistration reg = this.bgp.registerUpdateListener(new SimpleSessionListener());
		assertEquals(SimpleSessionListener.class, reg.getListener().getClass());
	}

	@After
	public void tearDown() {
		this.bgp.close();
	}
}
