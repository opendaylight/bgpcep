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
 * Factory for generating Session proposals. Used by a server. Interface needs to be implemented
 * by a protocol specific abstract class that will produce protocol specific Session Proposals.
 * The abstract class should be extended by the user in order to return particular object.
 *
 * Example:
 *
 * public abstract class PCEPSessionProposalFactory implements SessionProposalFactory { ... }
 *
 * public final class SimplePCEPSessionProposalFactory extends PCEPSessionProposalFactory { ... }
 */
public interface SessionProposalFactory {

	/**
	 * Returns session proposal.
	 *
	 * @param address
	 *            serves as constraint, so that factory is able to return
	 *            different proposals for different addresses
	 * @param sessionId
	 *            identifier of the session
	 * @return specific session proposal
	 */
	public SessionProposal getSessionProposal(final InetSocketAddress address, final int sessionId);
}
