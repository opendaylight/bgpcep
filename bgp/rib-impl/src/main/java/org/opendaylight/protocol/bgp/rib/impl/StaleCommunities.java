/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import java.util.AbstractList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;

/**
 * Utility class which prepends {@code LLGR_STALE} community in front of some other communities.
 */
class StaleCommunities extends AbstractList<Communities> {
    private static final class RandomAccess extends StaleCommunities implements java.util.RandomAccess {
        RandomAccess(final List<Communities> orig) {
            super(orig);
        }
    }

    static final Communities STALE_LLGR = (Communities) CommunityUtil.LLGR_STALE;

    private final List<Communities> orig;

    StaleCommunities(final List<Communities> orig) {
        this.orig = requireNonNull(orig);
    }

    static StaleCommunities create(final List<Communities> orig) {
        return orig instanceof java.util.RandomAccess ? new RandomAccess(orig) : new StaleCommunities(orig);
    }

    @Override
    public final boolean contains(final Object obj) {
        return STALE_LLGR.equals(obj) || orig.contains(obj);
    }

    @Override
    public final Communities get(final int index) {
        return index == 0 ? STALE_LLGR : orig.get(index - 1);
    }

    @Override
    public final int size() {
        return orig.size() + 1;
    }
}
