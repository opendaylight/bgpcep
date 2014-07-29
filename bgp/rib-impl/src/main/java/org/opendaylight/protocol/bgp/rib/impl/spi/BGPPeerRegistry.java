/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * Registry that contains configured bgp peers ready for when a bgp session is established with remote peer.
 * IP address is uses as a key for configured peers. TODO Is IP sufficient ID for peers ?
 */
public interface BGPPeerRegistry extends AutoCloseable {

    /**
     * Add configured peer, its IP address and preferences. To be used when a BGP session is established.
     *
     * @param ip address of remote peer
     * @param peer configured peer as ReusableBGPPeer
     * @param prefs session preferences for configured peer
     */
    void addPeer(IpAddress ip, ReusableBGPPeer peer, BGPSessionPreferences prefs);

    /**
     * Remove configured peer from registry.
     *
     * @param ip address of remote peer
     */
    void removePeer(IpAddress ip);

    /**
     * Check whether peer on provided IP address is present in this registry.
     *
     * @param ip address of remote peer
     * @return true if peer is present false otherwise
     */
    boolean isPeerConfigured(IpAddress ip);

    /**
     * Get configured peer after BGP session was successfully established. Called by negotiators.
     *
     * @param ip address of remote peer
     * @param sourceId BGP ID of peer that initiated the session (current device or remote peer)
     * @param remoteId BGP ID of peer that accepted the session (current device or remote peer)
     * @return configured Peer as BGP listener
     *
     * @throws BGPDocumentedException if session establishment cannot be finished successfully
     * @throws java.lang.IllegalStateException if there is no peer configured for provided ip address
     */
    BGPSessionListener getPeer(IpAddress ip, Ipv4Address sourceId, Ipv4Address remoteId) throws BGPDocumentedException;

    /**
     * @param ip address of remote peer
     * @return BGP session preferences for configured peer
     *
     * @throws java.lang.IllegalStateException if there is no peer configured for provided ip address
     */
    BGPSessionPreferences getPeerPreferences(IpAddress ip);

}
