/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

/**
 * Class representing a route consumer, which performs additional processing on it.
 * This is subclassed by {@link BGPPeer} to move routes towards a particular remote
 * peer, or by LocRibSink, which translates it and pushes it towards the datastore.
 *
 * We choose to make this an abstract class, as that lowers the method call overhead
 * very slightly.
 */
abstract class RouteSink {
    private final Predicate<PathAttributes> policy;
    private final UnsignedInteger routerId;

    protected RouteSink(final Predicate<PathAttributes> policy, final UnsignedInteger routerId) {
        this.policy = Preconditions.checkNotNull(policy);
        this.routerId = Preconditions.checkNotNull(routerId);
    }

    final UnsignedInteger getRouterId() {
        return routerId;
    }

    /**
     * Check whether a particular route is eligible for being exported to this sink.
     *
     * @param attributes route attributes
     * @return True if the route can be consumed, false otherwise.
     */
    final boolean canConsumeRoute(final PathAttributes attributes) {
        return policy.apply(attributes);
    }

    abstract void routeUpdated(Object nlri);
}
