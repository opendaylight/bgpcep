/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.util.Timeout;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.lsp.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPRequest {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPRequest.class);
    private static final long MINIMUM_ELAPSED_TIME = 1;

    enum State {
        UNSENT,
        UNACKED,
        DONE,
    }

    private final SettableFuture<OperationResult> future = SettableFuture.create();
    private final Metadata metadata;

    @GuardedBy("this")
    private final Stopwatch stopwatch = Stopwatch.createStarted();
    @GuardedBy("this")
    private Timeout timeout;

    // FIXME; use atomic ops for this
    private volatile State state = State.UNSENT;

    PCEPRequest(final Metadata metadata, final Timeout timeout) {
        this.metadata = metadata;
        this.timeout = timeout;
    }

    ListenableFuture<OperationResult> getFuture() {
        return future;
    }

    Metadata getMetadata() {
        return metadata;
    }

    State getState() {
        return state;
    }

    synchronized void done(final OperationResult result) {
        if (state != State.DONE) {
            LOG.debug("Request went from {} to {}", state, State.DONE);
            state = State.DONE;
            if (timeout != null) {
                timeout.cancel();
                timeout = null;
            }
            future.set(result);
        }
    }

    synchronized void done() {
        OperationResult result;
        switch (state) {
            case UNSENT:
                result = OperationResults.UNSENT;
                break;
            case UNACKED:
                result = OperationResults.NOACK;
                break;
            case DONE:
                return;
            default:
                return;
        }
        done(result);
    }

    synchronized void sent() {
        if (state == State.UNSENT) {
            LOG.debug("Request went from {} to {}", state, State.UNACKED);
            state = State.UNACKED;
        }
    }

    synchronized long getElapsedMillis() {
        final long elapsedNanos = stopwatch.elapsed().toNanos();
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

        // FIXME: this is weird: it scales (0,1) up to 1, but otherwise scales down
        return elapsedMillis == 0 && elapsedNanos > 0 ? MINIMUM_ELAPSED_TIME : elapsedMillis;
    }
}
