/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;

@Deprecated
public interface CacheDisconnectedPeers {

    /**
     * Check whether Peer is inside the cache List
     *
     * @param peerId of destination peer
     * @return True if peer is contained on CacheList
     */
    boolean isPeerDisconnected(PeerId peerId);

    /**
     * Remove Peer from cache in case of reconnection
     *
     * @param peerId of reconnected peer
     */
    void reconnected(PeerId peerId);

    /**
     * Add to cache list disconnected peer
     *
     * @param peerId of disconnected peer
     */
    void insertDesconectedPeer(Ipv4Address peerId);
}
