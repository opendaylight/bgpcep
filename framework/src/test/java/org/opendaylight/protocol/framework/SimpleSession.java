/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;

public final class SimpleSession implements ProtocolSession {

	private final SessionListener listener;

	private final SessionParent d;

	private final int maxMsgSize;

	public SimpleSession(final ProtocolConnection connection, final SessionParent d, final int maxMsgSize) {
		this.listener = connection.getListener();
		this.d = d;
		this.maxMsgSize = maxMsgSize;
	}

	@Override
	public void close() throws IOException {
		this.d.onSessionClosed(this);
	}

	@Override
	public void startSession() {
		((SimpleSessionListener) this.listener).onSessionUp(this, null, null);
	}

	@Override
	public void handleMessage(final ProtocolMessage msg) {
	}

	@Override
	public void handleMalformedMessage(final DeserializerException e) {
	}

	@Override
	public void handleMalformedMessage(final DocumentedException e) {
	}

	@Override
	public void endOfInput() {
	}

	@Override
	public ProtocolMessageFactory getMessageFactory() {
		return null;
	}

	@Override
	public void onConnectionFailed(final IOException e) {
		((SimpleSessionListener) this.listener).onConnectionFailed(this, e);
	}

	@Override
	public int maximumMessageSize() {
		return this.maxMsgSize;
	}
}
