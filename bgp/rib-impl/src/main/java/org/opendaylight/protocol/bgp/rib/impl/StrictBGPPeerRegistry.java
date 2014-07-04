/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;

@ThreadSafe
public final class StrictBGPPeerRegistry implements BGPPeerRegistry {

    // TODO remove backwards compatibility
    public static StrictBGPPeerRegistry GLOBAL = new StrictBGPPeerRegistry();

    // TODO is PEER identified by IP sufficient ?
    @GuardedBy("this")
    private final Map<IpAddress, ReusableBGPPeer> peers = Maps.newHashMap();
    @GuardedBy("this")
    private final Map<IpAddress, SessionConnection> peerConnections = Maps.newHashMap();
    @GuardedBy("this")
    private final Map<IpAddress, BGPSessionPreferences> peerPreferences = Maps.newHashMap();

    @Override
    public synchronized void addPeer(final IpAddress ip, final ReusableBGPPeer peer, final BGPSessionPreferences preferences) {
        Preconditions.checkNotNull(ip);
        Preconditions.checkArgument(peers.containsKey(ip) == false, "Peer for %s already present", ip);
        peers.put(ip, Preconditions.checkNotNull(peer));
        peerPreferences.put(ip, Preconditions.checkNotNull(preferences));
    }

    @Override
    public synchronized void removePeer(final IpAddress ip) {
        Preconditions.checkNotNull(ip);
        peers.remove(ip);
    }

    @Override
    public boolean isPeerConfigured(final IpAddress ip) {
        Preconditions.checkNotNull(ip);
        return peers.containsKey(ip);
    }

    @Override
    public synchronized BGPSessionListener getPeer(final IpAddress ip,
            final Ipv4Address sourceId, final Ipv4Address destinationId)
            throws BGPDocumentedException {
        Preconditions.checkNotNull(ip);
        Preconditions.checkNotNull(sourceId);
        Preconditions.checkNotNull(destinationId);

        // TODO should this be BGP exception instead of IllegalState ?
        Preconditions.checkState(peers.containsKey(ip), "BGP peer with ip: %s not allowed, allowed peers are: %s", ip, peers.keySet());

        // TODO logging
        final SessionConnection currentConnection = new SessionConnection(sourceId, destinationId);

        if (peerConnections.containsKey(ip)) {
            final SessionConnection previousConnection = peerConnections.get(ip);

            // Session reestablished with different ids
            if(previousConnection.equals(currentConnection) == false) {
                throw new BGPDocumentedException(
                        String.format("BGP session with %s initiated from %s to %s has to be dropped. Same session already present from %s to %s",
                                ip, sourceId, destinationId, previousConnection.from, previousConnection.to),
                        BGPError.CEASE);
            // Session reestablished with lower source bgp id, dropping current
            } else if (previousConnection.isHigherDirection(currentConnection)) {
                throw new BGPDocumentedException(
                        String.format("BGP session with %s initiated from %s to %s has to be dropped. Opposite session already present",
                                ip, sourceId, destinationId),
                        BGPError.CEASE);
            // Session reestablished with higher source bgp id, dropping previous
            } else if (currentConnection.isHigherDirection(previousConnection)) {
                peers.get(ip).releaseConnection();
                return peers.get(ip);
            // Session reestablished with same source bgp id, dropping current as duplicate
            } else {
                throw new BGPDocumentedException(
                        String.format("BGP session with %s initiated from %s to %s has to be dropped. Same session already present",
                                ip, sourceId, destinationId),
                        BGPError.CEASE);
            }

        } else {
            peerConnections.put(ip, currentConnection);
            return peers.get(ip);
        }
    }

    @Override
    public BGPSessionPreferences getPeerPreferences(final IpAddress ip) {
        Preconditions.checkNotNull(ip);
        Preconditions.checkArgument(peerPreferences.containsKey(ip),
                "BGP peer with ip: %s not allowed, allowed peers are: %s", ip,
                peerPreferences.keySet());
        return peerPreferences.get(ip);
    }

    /**
     * Create IpAddress from SocketAddress. Only InetSocketAddress is accepted with inner address: Inet4Address and Inet6Address.
     *
     * @throws IllegalArgumentException if submitted socket address is not InetSocketAddress[ipv4 | ipv6]
     * @param socketAddress socket address to transform
     */
    public static IpAddress getIpAddress(final SocketAddress socketAddress) {
        Preconditions.checkNotNull(socketAddress);
        Preconditions.checkArgument(socketAddress instanceof InetSocketAddress, "Expecting InetSocketAddress but was %s", socketAddress.getClass());
        final InetAddress inetAddress = ((InetSocketAddress) socketAddress).getAddress();

        if(inetAddress instanceof Inet4Address) {
            return new IpAddress(new Ipv4Address(inetAddress.getHostAddress()));
        } else if(inetAddress instanceof Inet6Address) {
            return new IpAddress(new Ipv6Address(inetAddress.getHostAddress()));
        }

        throw new IllegalArgumentException("Expecting " + Inet4Address.class + " or " + Inet6Address.class + " but was " + inetAddress.getClass());
    }

    @Override
    public synchronized void close() throws Exception {
        peers.clear();
        peerConnections.clear();
    }

    /**
     * Session identifier that contains (source Bgp Id) -> (destination Bgp Id)
     */
    private static final class SessionConnection {
        private final Ipv4Address from, to;

        SessionConnection(final Ipv4Address from, final Ipv4Address to) {
            this.from = Preconditions.checkNotNull(from);
            this.to = Preconditions.checkNotNull(to);
        }

        /**
         * Equals does not take direction of connection into account id1 -> id2 and id2 -> id1 are equal
         */
        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final SessionConnection sessionConnection = (SessionConnection) o;

            if (!from.equals(sessionConnection.from) && !from.equals(sessionConnection.to)) return false;
            if (!to.equals(sessionConnection.to) && !to.equals(sessionConnection.from)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = from.hashCode() + to.hashCode();
            result = 31 * result;
            return result;
        }

        /**
         * Check if this connection is equal to other and if it contains higher source bgp id
         */
        boolean isHigherDirection(final SessionConnection other) {
            Preconditions.checkState(this.isSameDirection(other) == false, "Equal sessions with same direction");
            return toLong(from) > toLong(other.from);
        }

        private long toLong(final Ipv4Address from) {
            return Long.valueOf(from.getValue().replaceAll("[^0-9]", ""));
        }

        /**
         * Check if 2 connections are equal and face same direction
         */
        boolean isSameDirection(final SessionConnection other) {
            Preconditions.checkState(this.equals(other), "Only equal sessions can be compared");
            return from.equals(other.from);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("from", from)
                    .add("to", to)
                    .toString();
        }
    }
}
