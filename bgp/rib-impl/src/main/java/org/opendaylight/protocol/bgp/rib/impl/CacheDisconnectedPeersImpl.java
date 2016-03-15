/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.bgp.rib.spi.CacheDisconnectedPeers;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;

public final class CacheDisconnectedPeersImpl implements CacheDisconnectedPeers {
    private static Cache<PeerId, Boolean> cache = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();

    CacheDisconnectedPeersImpl() {
    }

    @Override
    public boolean isPeerDisconnected(final PeerId peerId) {
        if (this.cache.getIfPresent(peerId) != null) {
            return true;
        }
        return false;
    }

    @Override
    public void reconnected(final PeerId peerId) {
        this.cache.asMap().remove(peerId);
    }

    @Override
    public void insertDesconectedPeer(final Ipv4Address peerId) {
        this.cache.put(RouterIds.createPeerId(peerId), true);
    }
}
