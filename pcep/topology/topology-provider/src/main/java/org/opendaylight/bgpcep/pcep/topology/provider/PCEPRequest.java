/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.util.Timeout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.lsp.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPRequest {
    /**
     * Logical state of a {@link PCEPRequest}.
     */
    enum State {
        /**
         * The request has not been written out to the session.
         */
        UNSENT,
        /**
         * The request has been sent to to the sesssion, but has not been acknowledged by the peer.
         */
        UNACKED,
        /**
         * The request has been completed.
         */
        DONE,
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPRequest.class);
    private static final long MINIMUM_ELAPSED_TIME = 1;
    private static final VarHandle STATE;

    static {
        try {
            STATE = MethodHandles.lookup().findVarHandle(PCEPRequest.class, "state", State.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final SettableFuture<OperationResult> future = SettableFuture.create();
    private final long startNanos = System.nanoTime();
    private final Metadata metadata;

    // Manipulated via STATE
    @SuppressWarnings("unused")
    private volatile State state = State.UNSENT;

    // Guarded by state going to State.DONE
    private Timeout timeout;

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

    long getElapsedMillis() {
        final long elapsedNanos = System.nanoTime() - startNanos;
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

        // FIXME: this is weird: it scales (0,1) up to 1, but otherwise scales down
        return elapsedMillis == 0 && elapsedNanos > 0 ? MINIMUM_ELAPSED_TIME : elapsedMillis;
    }

    /**
     * Mark this request as {@link State#UNACKED} if it currently is {@link State#UNSENT}.
     */
    void markUnacked() {
        if (STATE.compareAndSet(this, State.UNSENT, State.UNACKED)) {
            LOG.debug("Request went from {} to {}", State.UNSENT, State.UNACKED);
        }
    }

    /**
     * Mark this request as {@link State#DONE} with specified {@link OperationResult}. If it is already done, this
     * method does nothing.
     *
     * @param result Result to report
     */
    void finish(final OperationResult result) {
        final var prev = setDone();
        if (prev != State.DONE) {
            setFuture(prev, result);
        }
    }

    /**
     * Mark this request as {@link State#DONE} with a result derived from its current state. If it is already done, this
     * method does nothing.
     *
     * @return Previous state
     */
    State cancel() {
        final var prev = setDone();
        // FIXME: exhaustive when we have JDK17+
        switch (prev) {
            case UNSENT:
                setFuture(prev, OperationResults.UNSENT);
                break;
            case UNACKED:
                setFuture(prev, OperationResults.NOACK);
                break;
            case DONE:
                // No-op
                break;
            default:
                throw new IllegalStateException("Unhandled state " + prev);
        }
        return prev;
    }

    private State setDone() {
        return (State) STATE.getAndSet(this, State.DONE);
    }

    private void setFuture(final State prev, final OperationResult result) {
        LOG.debug("Request went from {} to {}", prev, State.DONE);
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
        future.set(result);
    }
}
