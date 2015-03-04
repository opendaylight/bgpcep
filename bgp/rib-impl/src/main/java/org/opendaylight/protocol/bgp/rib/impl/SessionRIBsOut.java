/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;

final class SessionRIBsOut extends AbstractAdjRIBsOut implements Runnable {
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final BGPSessionImpl session;

    SessionRIBsOut(final BGPSessionImpl session) {
        this.session = Preconditions.checkNotNull(session);
    }

    @Override
    protected void wantWrite() {
        if (scheduled.compareAndSet(false, true)) {
            session.schedule(this);
        }
    }

    @Override
    protected boolean writePDU(final Update pdu) {
        session.writeAndFlush(pdu);
        return session.isWritable();
    }

    @Override
    public void run() {
        process();
    }
}
