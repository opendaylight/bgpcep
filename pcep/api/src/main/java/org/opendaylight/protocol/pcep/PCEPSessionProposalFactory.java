/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Factory for generating PCEP Session proposals. Used by a server.
 */
public interface PCEPSessionProposalFactory {
    /**
     * Creates Open with session proposal.
     *
     * @param address      serves as constraint, so that factory is able to return different proposals for different
     *                     addresses
     * @param sessionId    is used for creation of PCEPOpenObject
     * @param peerProposal for including information from peer to our Open message
     * @return specific session proposal
     */
    @NonNull Open getSessionProposal(@NonNull InetSocketAddress address, @NonNull Uint8 sessionId,
            @Nullable PCEPPeerProposal peerProposal);

    /**
     * Returns list containing PCEP Capabilities.
     *
     * @return PCEPCapabilities
     */
    @NonNull List<PCEPCapability> getCapabilities();
}
