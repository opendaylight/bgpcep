/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.SessionPreferencesCheckerFactory;

/**
 * Factory for generating PCEP Session Proposal Checkers. Used by a server.
 */
public abstract class PCEPSessionProposalCheckerFactory implements SessionPreferencesCheckerFactory {

	/**
	 * Returns one session proposal checker that is registered to this factory
	 * @param address serves as constraint, so that factory is able to
	 * return different checkers for different factories
	 * @return specific session proposal checker
	 */
	@Override
	public abstract PCEPSessionProposalChecker getPreferencesChecker(InetSocketAddress address);
}
