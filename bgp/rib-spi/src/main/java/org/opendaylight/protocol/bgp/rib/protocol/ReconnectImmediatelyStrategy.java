/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.rib.protocol;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 24.6.2015.
 */
@ThreadSafe
public final class ReconnectImmediatelyStrategy implements ReconnectStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(ReconnectImmediatelyStrategy.class);
    private final EventExecutor executor;
    private final int timeout;

    public ReconnectImmediatelyStrategy(final EventExecutor executor, final int timeout) {
        Preconditions.checkArgument(timeout >= 0);
        this.executor = Preconditions.checkNotNull(executor);
        this.timeout = timeout;
    }

    @Override
    public Future<Void> scheduleReconnect(final Throwable cause) {
        LOG.debug("Connection attempt failed", cause);
        return executor.newSucceededFuture(null);
    }

    @Override
    public void reconnectSuccessful() {
        // Nothing to do
    }

    @Override
    public int getConnectTimeout() {
        return timeout;
    }
}