/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Timer;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.concepts.ASNumber;

/**
 * Mock of the BGP speakers session.
 */
public class SpeakerSessionMock extends BGPSessionImpl {

	private final BGPSessionListener client;

	SpeakerSessionMock(final BGPSessionListener listener, final BGPSessionListener client) {
		super(new MockDispatcher(), new Timer(), new BGPConnectionImpl(null, listener, new BGPSessionPreferences(new ASNumber(30), (short) 15, null, null), new BGPSessionProposalCheckerImpl()), 3, null, null);
		this.client = client;
	}

	@Override
	public void sendMessage(final BGPMessage msg) {
		this.lastMessageSentAt = System.nanoTime();
		this.client.onMessage(msg);
	}
}
