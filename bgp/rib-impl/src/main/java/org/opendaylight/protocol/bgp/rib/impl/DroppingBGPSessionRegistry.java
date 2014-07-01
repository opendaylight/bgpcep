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
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.rib.impl.spi.GlobalBGPSessionRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
 * Session registry that performs checks for duplicate bgp connections.
 *
 * In case duplicate is detected, one of the sessions is dropped depending on BGP Ids.
 */
@ThreadSafe
public final class DroppingBGPSessionRegistry implements GlobalBGPSessionRegistry {

    @GuardedBy("this")
    private final Map<SessionId, SessionWithId> sessions;
    private static GlobalBGPSessionRegistry instance;

    public DroppingBGPSessionRegistry() {
        sessions = Maps.newHashMap();
    }

    // TODO remove singleton instance, make this registry part of config subsystem (We have to make sure all peers get the same instance)
    public static GlobalBGPSessionRegistry getInstance() {
        if(instance == null) {
            instance = new org.opendaylight.protocol.bgp.rib.impl.DroppingBGPSessionRegistry();
        }
        return instance;
    }

    /**
     * https://blog.initialdraft.com/archives/3960/
     */
    @Override
    public synchronized RegistrationResult addSession(final BGPSession session, final Ipv4Address fromId, final Ipv4Address toId) {
        final SessionId currentSessionId = new SessionId(fromId, toId);

        if(sessions.containsKey(currentSessionId)) {
            final SessionWithId previousSessionId = sessions.get(currentSessionId);
            // TODO do not close session directly, give call to peer
            if(currentSessionId.isSameDirection(previousSessionId.getId())) {
                session.close();
                return RegistrationResult.DUPLICATE;
            } else if(currentSessionId.isHigher(previousSessionId.getId())) {
                previousSessionId.getSession().close();
                return RegistrationResult.DROPPED_PREVIOUS;
            } else if(previousSessionId.getId().isHigher(currentSessionId)){
                session.close();
                return RegistrationResult.DROPPED;
            }
        }

        this.sessions.put(currentSessionId, new SessionWithId(currentSessionId, session));
        return RegistrationResult.SUCCESS;
    }

    @Override
    public synchronized void removeSession(final Ipv4Address fromId, final Ipv4Address toId) {
        final SessionId sessionId = new SessionId(fromId, toId);
        Preconditions.checkArgument(sessions.containsKey(sessionId), "No session from {} to {} present in registry", fromId, toId);
        sessions.remove(sessionId);
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
    private static final class SessionWithId {
        private final BGPSession session;
        private final SessionId id;

        private SessionWithId(final SessionId id, final BGPSession session) {
            this.id = id;
            this.session = session;
        }

        public BGPSession getSession() {
            return session;
        }

        public SessionId getId() {
            return id;
        }
    }
}
