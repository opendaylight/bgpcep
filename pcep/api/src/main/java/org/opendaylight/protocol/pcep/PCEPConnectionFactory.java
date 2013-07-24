/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.ProtocolConnectionFactory;

public interface PCEPConnectionFactory extends ProtocolConnectionFactory {
	@Override
	public PCEPConnection createProtocolConnection(final InetSocketAddress address);

	public void setProposal(final PCEPSessionProposalFactory proposals, final InetSocketAddress address, final int sessionId);
}
