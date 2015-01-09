/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

final class RouterIds {
    private static final LoadingCache<String, UnsignedInteger> ROUTER_IDS = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<String, UnsignedInteger>() {
        @Override
        public UnsignedInteger load(final String key) {
            return UnsignedInteger.fromIntBits(InetAddresses.coerceToInteger(InetAddresses.forString(key)));
        }
    });

    private RouterIds() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get a router ID in unsigned integer format from an Ipv4Address. This implementation uses an internal
     * cache, so the objects can be expected to perform quickly when compared with equals and similar.
     *
     * @param address Router ID as a dotted-quad
     * @return Router ID as an {@link UnsignedInteger}
     */
    static UnsignedInteger routerIdForAddress(@Nonnull final Ipv4Address address) {
        return ROUTER_IDS.getUnchecked(address.getValue());
    }
}
