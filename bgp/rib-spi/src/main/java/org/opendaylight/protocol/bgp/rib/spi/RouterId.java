/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.InetAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * The concept of a Router Identifier.
 *
 * @author Robert Varga
 */
public final class RouterId implements Comparable<RouterId>, Immutable {
    static final String PEER_ID_PREFIX = "bgp://";

    private static final int PEER_ID_PREFIX_LENGTH = 6;

    static {
        verify(PEER_ID_PREFIX.length() == PEER_ID_PREFIX_LENGTH);
    }

    private static final LoadingCache<PeerId, RouterId> BY_PEER_ID = CacheBuilder.newBuilder().weakValues()
            .build(new CacheLoader<PeerId, RouterId>() {
                @Override
                public RouterId load(final PeerId key) {
                    return new RouterId(key);
                }
            });
    private static final LoadingCache<String, RouterId> BY_DOTTED_QUAD =
            CacheBuilder.newBuilder().weakValues().build(new CacheLoader<String, RouterId>() {
                @Override
                public RouterId load(final String key) {
                    final InetAddress addr = InetAddresses.forString(key);
                    checkArgument(addr instanceof Inet4Address, "Invalid address %s", key);
                    return BY_PEER_ID.getUnchecked(new PeerId(RouterId.PEER_ID_PREFIX.concat(key)));
                }
            });

    private final @NonNull PeerId peerId;
    private final int intBits;

    RouterId(final PeerId peerId) {
        this.peerId = requireNonNull(peerId);
        // This relies on peedId being initialized
        this.intBits = InetAddresses.coerceToInteger(InetAddresses.forString(toString()));
    }

    /**
     * Get a router ID in unsigned integer format from an Ipv4Address. This implementation uses an internal
     * cache, so the objects can be expected to perform quickly when compared with equals and similar.
     *
     * @param address Router ID as a dotted-quad
     * @return Router ID
     */
    public static RouterId forAddress(final String address) {
        return BY_DOTTED_QUAD.getUnchecked(address);
    }

    /**
     * Get a router ID in unsigned integer format from an Ipv4Address. This implementation uses an internal
     * cache, so the objects can be expected to perform quickly when compared with equals and similar.
     *
     * @param address Router ID as an Ipv4Address
     * @return Router ID
     */
    public static RouterId forAddress(final Ipv4Address address) {
        return forAddress(address.getValue());
    }

    public static RouterId forPeerId(final PeerId peerId) {
        checkArgument(peerId.getValue().startsWith(PEER_ID_PREFIX), "Unhandled peer ID %s", peerId);
        return BY_PEER_ID.getUnchecked(peerId);
    }

    public @NonNull PeerId getPeerId() {
        return peerId;
    }

    @Override
    public int hashCode() {
        return intBits;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof RouterId && intBits == ((RouterId) obj).intBits;
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public int compareTo(final RouterId o) {
        return Integer.compareUnsigned(intBits, o.intBits);
    }

    @Override
    public String toString()  {
        return peerId.getValue().substring(PEER_ID_PREFIX_LENGTH);
    }
}
