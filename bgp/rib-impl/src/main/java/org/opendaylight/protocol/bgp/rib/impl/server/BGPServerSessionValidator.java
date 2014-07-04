/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.server;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;

/**
 * Validates Bgp sessions established from remote device to current by consulting BGPPeerRegistry if such peer is registered.
 */
public final class BGPServerSessionValidator implements BGPSessionValidator {
    private final BGPPeerRegistry peerRegistry;

    public BGPServerSessionValidator(final BGPPeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
    }

    @Override
    public void validate(final Open openObj, final IpAddress addr) throws BGPDocumentedException {
        if (peerRegistry.isPeerConfigured(addr) == false) {
            throw new BGPDocumentedException("BGP Peer: " + addr + " not allowed, check configured peers", BGPError.CEASE);
        }
    }
}
