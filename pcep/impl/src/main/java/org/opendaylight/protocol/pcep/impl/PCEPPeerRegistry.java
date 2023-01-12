/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.yangtools.yang.common.Uint8;

// This class is thread-safe
final class PCEPPeerRegistry {

    /**
     * The maximum lifetime for which we should hold on to a session ID before assuming it is okay to reuse it.
     */
    private static final long ID_CACHE_SECONDS = 3 * 3600;

    /**
     * The total amount of time we should remember a peer having been present, unless some other pressure forces us to
     * forget about it due to {@link PEER_CACHE_SIZE}.
     */
    private static final long PEER_CACHE_SECONDS = 24 * 3600;

    /**
     * Maximum total number of peers we keep track of. Combined with {@link PEER_CACHE_SECONDS}, this defines how many
     * peers we can see turn around.
     */
    private static final long PEER_CACHE_SIZE = 1024;

    // FIXME: why do we hold a lock?!
    @GuardedBy("this")
    private final Cache<ByteArrayWrapper, PeerRecord> formerClients = CacheBuilder.newBuilder()
            .expireAfterAccess(PEER_CACHE_SECONDS, TimeUnit.SECONDS).maximumSize(PEER_CACHE_SIZE).build();

    @GuardedBy("this")
    private final Map<ByteArrayWrapper, SessionReference> sessions = new HashMap<>();

    protected interface SessionReference extends AutoCloseable {
        Uint8 getSessionId();
    }


    protected synchronized Optional<SessionReference> getSessionReference(final byte[] clientAddress) {
        final SessionReference sessionReference = sessions.get(new ByteArrayWrapper(clientAddress));
        if (sessionReference != null) {
            return Optional.of(sessionReference);
        }
        return Optional.empty();
    }

    protected synchronized Optional<SessionReference> removeSessionReference(final byte[] clientAddress) {
        return Optional.ofNullable(sessions.remove(new ByteArrayWrapper(clientAddress)));
    }

    protected synchronized void putSessionReference(final byte[] clientAddress,
            final SessionReference sessionReference) {
        sessions.put(new ByteArrayWrapper(clientAddress), sessionReference);
    }

    protected synchronized Uint8 nextSession(final byte[] clientAddress) throws ExecutionException {
        final PeerRecord peer =
            formerClients.get(new ByteArrayWrapper(clientAddress), () -> new PeerRecord(ID_CACHE_SECONDS, null));

        return peer.allocId();
    }

    protected synchronized void releaseSession(final byte[] clientAddress, final Uint8 sessionId)
            throws ExecutionException {
        formerClients.get(new ByteArrayWrapper(clientAddress), () -> new PeerRecord(ID_CACHE_SECONDS, sessionId));
    }

    private static final class ByteArrayWrapper {
        private final byte[] byteArray;

        ByteArrayWrapper(final byte[] byteArray) {
            this.byteArray = byteArray == null ? null : byteArray.clone();
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(byteArray);
        }

        @Override
        public boolean equals(final Object obj) {
            return this == obj || obj instanceof ByteArrayWrapper other && Arrays.equals(byteArray, other.byteArray);
        }
    }
}
