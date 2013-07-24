/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.io.IOException;

import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.SessionParent;

public class MockDispatcher implements SessionParent {

	@Override
	public void onSessionClosed(final ProtocolSession session) {

	}

	@Override
	public void checkOutputBuffer(final ProtocolSession session) {

	}

	@Override
	public void close() throws IOException {
	}
}
