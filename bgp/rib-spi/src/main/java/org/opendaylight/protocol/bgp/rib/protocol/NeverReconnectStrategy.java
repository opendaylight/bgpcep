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

/**
 * Created by cgasparini on 22.6.2015.
 */
@ThreadSafe
public final class NeverReconnectStrategy implements ReconnectStrategy {
    private final EventExecutor executor;
    private final int timeout;

    public NeverReconnectStrategy(EventExecutor executor, int timeout) {
        Preconditions.checkArgument(timeout >= 0);
        this.executor = (EventExecutor)Preconditions.checkNotNull(executor);
        this.timeout = timeout;
    }

    public Future<Void> scheduleReconnect(Throwable cause) {
        return this.executor.newFailedFuture(new Throwable());
    }

    public void reconnectSuccessful() {
    }

    public int getConnectTimeout() {
        return this.timeout;
    }
}
