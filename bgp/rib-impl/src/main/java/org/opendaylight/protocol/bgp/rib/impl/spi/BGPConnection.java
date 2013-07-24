/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;

import org.opendaylight.protocol.framework.ProtocolConnection;

/**
 * BGP specific connection attributes.
 */
public interface BGPConnection extends ProtocolConnection {

	@Override
	public BGPSessionListener getListener();

	@Override
	public BGPSessionPreferences getProposal();

	@Override
	public BGPSessionProposalChecker getProposalChecker();
}
