/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;


/**
 * Mock implementation of {@link BGPListener} for testing purposes.
 */
final class BGPListenerMock extends BGPSessionListener {
	private final List<BGPMessage> buffer = Collections.synchronizedList(new ArrayList<BGPMessage>());
	private boolean connected = false;

	protected List<BGPMessage> getBuffer() {
		return this.buffer;
	}

	protected boolean isConnected() {
		return this.connected;
	}

	@Override
	public void onMessage(final BGPMessage message) {
		this.buffer.add(message);
	}

	@Override
	public void onSessionUp(final Set<BGPTableType> remoteParams) {
		this.connected = true;
	}

	@Override
	public void onSessionDown(final BGPSession session, final Exception e) {
		this.connected = false;
	}

	@Override
	public void onSessionTerminated(final BGPError cause) {
		this.connected = false;
	}
}
