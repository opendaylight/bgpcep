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
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class PeerRecord {
    @GuardedBy("this")
    private final Cache<Short, Short> pastIds;

    @GuardedBy("this")
    private Short lastId;

    PeerRecord(final long idLifetimeSeconds, final Short lastId) {
        // Note that the cache is limited to 255 entries -- which means we will always have
        // a single entry available. That number will be the Last Recently Used ID.
        this.pastIds = CacheBuilder.newBuilder().expireAfterWrite(idLifetimeSeconds, TimeUnit.SECONDS).maximumSize(255).build();
        this.lastId = lastId;
    }

    synchronized Short allocId() {
        Short id = this.lastId == null ? 0 : this.lastId;

        while (this.pastIds.getIfPresent(id) != null) {
            id = (short) ((id + 1) % 255);
        }

        this.pastIds.put(id, id);
        this.lastId = id;
        return id;
    }
}
