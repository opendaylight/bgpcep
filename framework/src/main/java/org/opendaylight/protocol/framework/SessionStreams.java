/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.TimerTask;

/**
 * DTO object to be attached to socket channel. Contains session and
 * streams associated with the socket channel.
 */
final class SessionStreams {

	private final ProtocolInputStream inputStream;

	private final PipedInputStream pipedInputStream;

	private final PipedOutputStream pipedOutputStream;

	private final ProtocolSession session;

	final ProtocolConnection connection;
	int connectCount = 0;
	TimerTask timer = null;

	SessionStreams(PipedOutputStream pipedOutputStream,
			PipedInputStream pipedInputStream,
			ProtocolInputStream inputStream,
			ProtocolSession session, ProtocolConnection connection) {
		this.pipedOutputStream = pipedOutputStream;
		this.pipedInputStream = pipedInputStream;
		this.inputStream = inputStream;
		this.session = session;
		this.connection = connection;
	}

	PipedOutputStream getPipedOutputStream() {
		return this.pipedOutputStream;
	}

	PipedInputStream getPipedInputStream() {
		return this.pipedInputStream;
	}

	ProtocolInputStream getProtocolInputStream() {
		return this.inputStream;
	}

	ProtocolSession getSession() {
		return this.session;
	}
}
