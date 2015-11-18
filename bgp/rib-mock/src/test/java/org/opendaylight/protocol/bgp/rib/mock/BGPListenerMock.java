/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Mock implementation of {@link BGPSessionListener} for testing purposes.
 */
public final class BGPListenerMock implements BGPSessionListener {
    private final List<Notification> buffer = Collections.synchronizedList(new ArrayList<Notification>());
    private boolean connected = false;

    protected List<Notification> getBuffer() {
        return this.buffer;
    }

    protected boolean isConnected() {
        return this.connected;
    }

    @Override
    public void onMessage(final BGPSession session, final Notification message) {
        this.buffer.add(message);
    }

    @Override
    public void releaseConnection() {
    }

    @Override
    public void onSessionUp(final BGPSession session) {
        this.connected = true;
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        this.connected = false;

    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason reason) {
        this.connected = false;
    }

    @Override
    public boolean isSessionActive() {
        return true;
    }

    @Override
    public void markUptodate(final TablesKey tablesKey) {
    }
}
