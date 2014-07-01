/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.server;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.protocol.bgp.rib.impl.spi.ConfiguredPeer;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;

@ThreadSafe
public final class DroppingBGPPeerRegistry implements BGPSessionValidator, BGPPeerRegistry {

    @GuardedBy("this")
    private final Set<IpAddress> permittedAddresses = Sets.newHashSet();

    @GuardedBy("this")
    private final Map<SessionId, PeerWithId> sessions = Maps.newHashMap();

    @Override
    public synchronized void validate(final Open openObj, final IpAddress addr) throws BGPDocumentedException {
        if(permittedAddresses.contains(addr) == false) {
            throw new BGPDocumentedException("BGP Peer: " + addr + " not allowed, allowed peers: " + permittedAddresses, BGPError.CEASE);
        }
    }



    @Override
    public synchronized void addPeer(final ConfiguredPeer peer) {
        final IpAddress addr = peer.getRemoteAddress();
        permittedAddresses.add(addr);
    }

    @Override
    public RegistrationResult peerUp(final Peer peer, final Ipv4Address fromId, final Ipv4Address toId) {
        final SessionId currentSessionId = new SessionId(fromId, toId);

        if(sessions.containsKey(currentSessionId)) {
            final PeerWithId previousSessionId = sessions.get(currentSessionId);
            // TODO do not close session directly, give call to peer
            if(currentSessionId.isSameDirection(previousSessionId.getId())) {
                peer.drop();
                return RegistrationResult.DUPLICATE;
            } else if(currentSessionId.isHigher(previousSessionId.getId())) {
                previousSessionId.getSession().drop();
                previousSessionId.getSession();
                return RegistrationResult.DROPPED_PREVIOUS;
            } else if(previousSessionId.getId().isHigher(currentSessionId)){
                peer.drop();
                return RegistrationResult.DROPPED;
            }
        }

        this.sessions.put(currentSessionId, new PeerWithId(currentSessionId, peer));
        return RegistrationResult.SUCCESS;
    }

    @Override
    public void peerDown(final Ipv4Address fromId, final Ipv4Address toId) {

    }

    @Override
    public synchronized void removePeer(final ConfiguredPeer peer) {
        permittedAddresses.remove(peer.getRemoteAddress());
    }

    /**
     * Session identifier that contains (source Bgp Id) -> (destination Bgp Id)
     */
    private static final class SessionId {
        private final Ipv4Address from, to;

        SessionId(final Ipv4Address from, final Ipv4Address to) {
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

            final SessionId sessionId = (SessionId) o;

            if (!from.equals(sessionId.from) && !from.equals(sessionId.to)) return false;
            if (!to.equals(sessionId.to) && !to.equals(sessionId.from)) return false;

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
        boolean isHigher(final SessionId other) {
            Preconditions.checkState(this.isSameDirection(other) == false, "Equal sessions with same direction");
            return toLong(from) > toLong(other.from);
        }

        private long toLong(final Ipv4Address from) {
            return Long.valueOf(from.getValue().replaceAll("[^0-9]", ""));
        }

        /**
         * Check if 2 connections are equal and face same direction
         */
        boolean isSameDirection(final SessionId other) {
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

    /**
     * DTO for BGPSession + SessionId
     */
    private static final class PeerWithId {
        private final Peer session;
        private final SessionId id;

        private PeerWithId(final SessionId id, final Peer session) {
            this.id = id;
            this.session = session;
        }

        public Peer getSession() {
            return session;
        }

        public SessionId getId() {
            return id;
        }
    }
}
