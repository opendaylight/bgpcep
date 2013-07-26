/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;

import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPConnection;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

public class ServerSessionMock extends PCEPSessionImpl {

	private final MockPCE client;

	public ServerSessionMock(final PCEPSessionListener listener, final PCEPSessionListener client) {
		super(new MockDispatcher(), new Timer(), new PCEPConnection() {
			@Override
			public InetSocketAddress getPeerAddress() {
				try {
					return new InetSocketAddress(InetAddress.getByName("localhost"), 4189);
				} catch (final UnknownHostException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			public PCEPSessionListener getListener() {
				return listener;
			}

			@Override
			public PCEPSessionPreferences getProposal() {
				return new PCEPSessionPreferences(new PCEPOpenObject(4, 9, 2));
			}

			@Override
			public PCEPSessionProposalChecker getProposalChecker() {
				return new SimpleSessionProposalChecker();
			}
		}, new PCEPMessageFactory(), 5, 30, null);
		this.client = (MockPCE) client;
	}

	@Override
	public void sendMessage(final PCEPMessage msg) {
		this.lastMessageSentAt = System.nanoTime();
		this.client.onMessage(this, msg);
	}

	@Override
	public void close() {
		this.client.onSessionTerminated(this, new PCEPCloseTermination(Reason.UNKNOWN));
	}
}
