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
import java.util.concurrent.TimeUnit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.lsp.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPRequest {
    static enum State {
        UNSENT,
        UNACKED,
        DONE,
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPRequest.class);
    private final SettableFuture<OperationResult> future;
    private final Metadata metadata;
    private volatile State state;
    private final Stopwatch stopwatch;

    PCEPRequest(final Metadata metadata) {
        this.future = SettableFuture.create();
        this.metadata = metadata;
        this.state = State.UNSENT;
        this.stopwatch = Stopwatch.createStarted();
    }

    protected ListenableFuture<OperationResult> getFuture() {
        return future;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public State getState() {
        return state;
    }

    public synchronized void done(final OperationResult result) {
        if (state != State.DONE) {
            LOG.debug("Request went from {} to {}", state, State.DONE);
            state = State.DONE;
            future.set(result);
        }
    }

    public synchronized void sent() {
        if (state == State.UNSENT) {
            LOG.debug("Request went from {} to {}", state, State.UNACKED);
            state = State.UNACKED;
        }
    }

    public long getElapsedMillis() {
        return this.stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }
}
