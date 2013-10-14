/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.mockito.Mockito.mock;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;

import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.open.message.OpenBuilder;

public class ServerSessionMock extends PCEPSessionImpl {

	private final MockPCE client;

	public ServerSessionMock(final PCEPSessionListener listener, final PCEPSessionListener client) {
		super(new HashedWheelTimer(), listener, 5, mock(Channel.class), new OpenBuilder().setKeepalive((short) 4).setDeadTimer((short) 9).setSessionId(
				(short) 2).build(), new OpenBuilder().setKeepalive((short) 4).setDeadTimer((short) 9).setSessionId((short) 2).build());
		this.client = (MockPCE) client;
	}

	@Override
	public void sendMessage(final Message msg) {
		this.lastMessageSentAt = System.nanoTime();
		this.client.onMessage(this, msg);
	}

	@Override
	public void close() {
		this.client.onSessionTerminated(this, new PCEPCloseTermination(TerminationReason.Unknown));
	}
}
