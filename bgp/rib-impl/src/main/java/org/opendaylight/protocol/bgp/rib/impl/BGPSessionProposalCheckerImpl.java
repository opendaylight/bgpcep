/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.io.Closeable;
import java.io.IOException;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposalChecker;

import org.opendaylight.protocol.framework.SessionPreferences;

/**
 * Basic implementation of BGP Session Proposal Checker. Session characteristics are always acceptable.
 */
public final class BGPSessionProposalCheckerImpl extends BGPSessionProposalChecker implements Closeable {

	public BGPSessionProposalCheckerImpl() {

	}

	@Override
	public SessionPreferences getNewProposal(final SessionPreferences oldOpen) {
		throw new IllegalStateException("This method shoudln't be called in BGP.");
	}

	@Override
	public Boolean checkSessionCharacteristics(final SessionPreferences openObj) throws BGPDocumentedException {
		return true;
	}

	@Override
	public void close() throws IOException {
		// nothing to close
	}
}
