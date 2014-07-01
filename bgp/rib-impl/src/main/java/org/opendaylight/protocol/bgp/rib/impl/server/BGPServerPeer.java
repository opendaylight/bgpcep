/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.server;

import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.AbstractBGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * Class representing a server peer. Server peer connection orientation: (remote BGP id) -> (local BGP id)
 */
public final class BGPServerPeer extends AbstractBGPPeer implements BGPSessionListener, Peer, AutoCloseable {

    public BGPServerPeer(final RIB rib, final BGPPeerRegistry sessionRegistry) {
        super("bgp-peer-remote", rib, sessionRegistry);
    }

    @Override
    protected Ipv4Address getSourceBgpId(final BGPSession session, final RIB rib) {
        return session.getBgpId();
    }

    @Override
    protected Ipv4Address getDestinationBgpId(final BGPSession session, final RIB rib) {
        return rib.getBgpIdentifier();
    }

}
