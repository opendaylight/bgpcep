/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;

public interface CacheDisconnectedPeers {

    /**
     * Check whether Peer is inside the cache List
     * @return True if peer is contained on CacheList
     */
    boolean isPeerDisconnected(PeerId peerId);

    /***
     * Remove Peer from cache in case of reconnection
     * @param peerId
     */
    void reconnected(PeerId peerId);

    /***
     * Insert disconnected peer to cache
     * @param peerId
     */
    void insertDisconnectedPeer(Ipv4Address peerId);
}
