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
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.protocol.concepts.ListenerRegistration;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;

public class BGPImplTest {

	@Mock
	private BGPDispatcher disp;

	@Mock
	private BGPSessionProposal prop;

	@Mock
	private Future<Void> future;

	private BGPImpl bgp;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		doReturn(this.future).when(this.disp).createReconnectingClient(any(InetSocketAddress.class), any(BGPSessionPreferences.class),
				any(BGPSessionListener.class), any(ReconnectStrategyFactory.class), any(ReconnectStrategy.class));
	}

	@Test
	public void testBgpImpl() throws Exception {
		doReturn(new BGPSessionPreferences(0, 0, null, Collections.<BgpParameters> emptyList())).when(this.prop).getProposal();
		this.bgp = new BGPImpl(this.disp, new InetSocketAddress(InetAddress.getLoopbackAddress(), 2000), this.prop);
		final ListenerRegistration<?> reg = this.bgp.registerUpdateListener(new SimpleSessionListener(),
				new ReconnectStrategyFactory() {
			@Override
			public ReconnectStrategy createReconnectStrategy() {
				return new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000);
			}
		}, new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
		assertEquals(SimpleSessionListener.class, reg.getListener().getClass());
	}

	@After
	public void tearDown() {
		this.bgp.close();
	}
}
