/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yangtools.yang.common.Uint8;

// This class is thread-safe
final class PeerRecord {
    @GuardedBy("this")
    // FIXME: why do we need to lock this?
    private final Cache<Short, Short> pastIds;

    @GuardedBy("this")
    private Uint8 lastId;

    PeerRecord(final long idLifetimeSeconds, final Uint8 lastId) {
        // Note that the cache is limited to 255 entries -- which means we will always have
        // a single entry available. That number will be the Last Recently Used ID.
        pastIds = CacheBuilder.newBuilder().expireAfterWrite(idLifetimeSeconds, TimeUnit.SECONDS)
                .maximumSize(Values.UNSIGNED_BYTE_MAX_VALUE).build();
        this.lastId = lastId;
    }

    synchronized Uint8 allocId() {
        short id = lastId == null ? 0 : lastId.toJava();

        while (pastIds.getIfPresent(id) != null) {
            id = (short) ((id + 1) % Values.UNSIGNED_BYTE_MAX_VALUE);
        }

        pastIds.put(id, id);

        final var ret = Uint8.valueOf(id);
        lastId = ret;
        return ret;
    }
}
