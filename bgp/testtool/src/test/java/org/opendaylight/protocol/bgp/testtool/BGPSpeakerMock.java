/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.opendaylight.protocol.bgp.parser.BGPMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPConnectionImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPInputStream;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalCheckerImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnection;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnectionFactory;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;

import org.opendaylight.protocol.framework.DispatcherImpl;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4;

public class BGPSpeakerMock {

	DispatcherImpl dispatcher;

	BGPSpeakerMock() throws IOException {
		this.dispatcher = new DispatcherImpl(Executors.defaultThreadFactory());
	}

	public static void main(final String[] args) throws IOException {

		final BGPSpeakerMock m = new BGPSpeakerMock();

		final BGPMessageParser parser = new BGPMessageFactory();

		m.dispatcher.createServer(new InetSocketAddress("127.0.0.2", 12345), new BGPConnectionFactory() {
			@Override
			public BGPConnection createProtocolConnection(final InetSocketAddress address) {
				final BGPSessionProposalImpl prop = new BGPSessionProposalImpl((short) 90, new ASNumber(25), IPv4.FAMILY.addressForString("127.0.0.2"));
				final BGPSessionPreferences prefs = prop.getProposal();
				try {
					prop.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
				return new BGPConnectionImpl(address, new SpeakerSessionListener(m.dispatcher), prefs, new BGPSessionProposalCheckerImpl());
			}
		}, new BGPSessionFactory(parser), BGPInputStream.FACTORY);
	}
}
