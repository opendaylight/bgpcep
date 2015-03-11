/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;

final class RouterIds {
    private static final LoadingCache<String, UnsignedInteger> ROUTER_IDS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<String, UnsignedInteger>() {
        @Override
        public UnsignedInteger load(final String key) {
            return UnsignedInteger.fromIntBits(InetAddresses.coerceToInteger(InetAddresses.forString(key)));
        }
    });
    private static final LoadingCache<PeerId, UnsignedInteger> BGP_ROUTER_IDS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<PeerId, UnsignedInteger>() {
        @Override
        public UnsignedInteger load(final PeerId key) {
            return routerIdForAddress(key.getValue().substring(BGP_PREFIX.length()));
        }
    });
    private static final String BGP_PREFIX = "bgp://";

    private RouterIds() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a router ID in unsigned integer format from an Ipv4Address. This implementation uses an internal
     * cache, so the objects can be expected to perform quickly when compared with equals and similar.
     *
     * @param address Router ID as a dotted-quad
     * @return Router ID as an {@link UnsignedInteger}
     */
    public static UnsignedInteger routerIdForAddress(@Nonnull final String address) {
        return ROUTER_IDS.getUnchecked(address);
    }

    public static UnsignedInteger routerIdForPeerId(@Nonnull final PeerId peerId) {
        Preconditions.checkArgument(peerId.getValue().startsWith(BGP_PREFIX), "Unhandled peer ID %s", peerId);
        return BGP_ROUTER_IDS.getUnchecked(peerId);
    }
}
