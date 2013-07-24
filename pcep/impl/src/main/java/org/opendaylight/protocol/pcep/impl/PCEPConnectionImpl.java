/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.pcep.PCEPConnection;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposalChecker;

public class PCEPConnectionImpl implements PCEPConnection {

	private final InetSocketAddress address;

	private final PCEPSessionListener listener;

	private final PCEPSessionPreferences proposal;

	private final PCEPSessionProposalChecker checker;

	public PCEPConnectionImpl(final InetSocketAddress address, final PCEPSessionListener listener, final PCEPSessionPreferences proposal,
			final PCEPSessionProposalChecker checker) {
		this.address = address;
		this.listener = listener;
		this.proposal = proposal;
		this.checker = checker;
	}

	@Override
	public InetSocketAddress getPeerAddress() {
		return this.address;
	}

	@Override
	public PCEPSessionListener getListener() {
		return this.listener;
	}

	@Override
	public PCEPSessionPreferences getProposal() {
		return this.proposal;
	}

	@Override
	public PCEPSessionProposalChecker getProposalChecker() {
		return this.checker;
	}
}
