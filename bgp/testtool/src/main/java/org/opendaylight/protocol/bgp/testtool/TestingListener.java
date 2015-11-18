/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing BGP Listener.
 */
public class TestingListener implements BGPSessionListener {
    private static final Logger LOG = LoggerFactory.getLogger(TestingListener.class);

    @Override
    public void onMessage(final BGPSession session, final Notification message) {
        LOG.info("Client Listener: message received: {}", message.toString());
    }

    @Override
    public void onSessionUp(final BGPSession session) {
        LOG.info("Client Listener: Session Up.");
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.info("Client Listener: Connection lost.");
        try {
            session.close();
        } catch (Exception ie) {
            LOG.warn("Error closing session", ie);
        }
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Client Listener: Connection lost: {}.", cause);
    }

    @Override
    public void releaseConnection() {
        LOG.info("Client Listener: Connection released.");
    }

    @Override
    public boolean isSessionActive() {
        return true;
    }

    @Override
    public void markUptodate(final TablesKey tablesKey) {
        LOG.debug("Table marked as up-to-date {}", tablesKey);
    }
}
