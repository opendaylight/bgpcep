/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.net.InetSocketAddress;

/**
 * Specifies connection attributes.
 */
public interface ProtocolConnection {

	/**
	 * Returns address to which the connection should bind.
	 * @return inet socket address
	 */
	InetSocketAddress getPeerAddress();

	/**
	 * Returns listener for the session.
	 * @return listener for the session
	 */
	SessionListener getListener();

	/**
	 * Returns session preferences (attributes for Open object).
	 * @return session preferences
	 */
	SessionPreferences getProposal();

	/**
	 * Returns session preferences checker.
	 * @return session preferences checker
	 */
	SessionPreferencesChecker getProposalChecker();
}
