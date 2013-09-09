/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import io.netty.util.HashedWheelTimer;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionNegotiatorFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.SessionListenerFactory;

public class BGPSpeakerMock extends AbstractDispatcher {

	public static void main(final String[] args) throws IOException {

		final BGPSpeakerMock m = new BGPSpeakerMock();

		final BGPSessionPreferences prefs = new BGPSessionProposalImpl((short) 90, new ASNumber(25), IPv4.FAMILY.addressForString("127.0.0.2")).getProposal();

		final SessionListenerFactory<BGPSessionListener> f = new SessionListenerFactory<BGPSessionListener>() {
			@Override
			public BGPSessionListener getSessionListener() {
				return new SpeakerSessionListener(m);
			}
		};

		m.createServer(new InetSocketAddress("127.0.0.2", 12345), f,
				new BGPSessionNegotiatorFactory(new HashedWheelTimer(), prefs), new BGPMessageFactory());
	}
}
