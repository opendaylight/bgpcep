/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer.route;

import com.google.common.base.Preconditions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.ZeroBasedCounter32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin Wang
 */
public class RouteCounter {
    private static final Logger LOG = LoggerFactory.getLogger(RouteCounter.class);

    private long count = 0L;

    public long increaseCount() {
        count++;
        LOG.debug("Route count is increased by one. Current count: {}", count);
        return count;
    }

    public long decreaseCount() {
        count--;
        LOG.debug("Route count is decreased by one. Current count: {}", count);
        Preconditions.checkState(count >= 0, "Route count must never be less than zero.");
        return count;
    }

    public void resetCount() {
        LOG.debug("Route count is reset to zero");
        count = 0L;
    }

    public ZeroBasedCounter32 toZeroBasedCounter32() {
        return new ZeroBasedCounter32(count);
    }
}
