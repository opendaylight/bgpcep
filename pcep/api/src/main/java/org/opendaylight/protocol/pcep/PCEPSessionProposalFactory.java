/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

/**
 * Factory for generating PCEP Session proposals. Used by a server.
 */
public interface PCEPSessionProposalFactory {

    /**
     * Returns one session proposal that is registered to this factory
     *
     * @param address serves as constraint, so that factory is able to return different proposals for different
     *        addresses
     * @param sessionId is used for creation of PCEPOpenObject
     * @return specific session proposal
     */
    Open getSessionProposal(InetSocketAddress address, int sessionId);

    /**
     *
     * @param address serves as constraint, so that factory is able to return different proposals for different
     *        addresses
     * @param sessionId is used for creation of PCEPOpenObject
     * @param peerProposal for including information from peer to our Open message
     * @return specific session proposal
     */
    Open getSessionProposal(InetSocketAddress address, int sessionId, PCEPPeerProposal peerProposal);
}
