/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPConnection;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposalChecker;


/**
 * Implementation of {@link BGPConnection}.
 */
public class BGPConnectionImpl implements BGPConnection {

	private final InetSocketAddress address;

	private final BGPSessionListener listener;

	private final BGPSessionPreferences proposal;

	private final BGPSessionProposalChecker checker;

	/**
	 * Merges together BGP specific connection attributes.
	 * 
	 * @param address inet socket address
	 * @param listener bgp session listener
	 * @param proposal bgp session preferences
	 * @param checker bgp session proposal checker
	 */
	public BGPConnectionImpl(final InetSocketAddress address, final BGPSessionListener listener, final BGPSessionPreferences proposal,
			final BGPSessionProposalChecker checker) {
		super();
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
	public BGPSessionListener getListener() {
		return this.listener;
	}

	@Override
	public BGPSessionPreferences getProposal() {
		return this.proposal;
	}

	@Override
	public BGPSessionProposalChecker getProposalChecker() {
		return this.checker;
	}
}
