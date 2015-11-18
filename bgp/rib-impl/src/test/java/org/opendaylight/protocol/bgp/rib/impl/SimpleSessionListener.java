/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the client.
 */
public class SimpleSessionListener implements BGPSessionListener {

    private final List<Notification> listMsg = Lists.newArrayList();

    public boolean up = false;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);

    public boolean down = false;

    private BGPSession session;

    public SimpleSessionListener() {
    }

    public List<Notification> getListMsg() {
        return this.listMsg;
    }

    @Override
    public void onMessage(final BGPSession session, final Notification message) {
        this.listMsg.add(message);
        LOG.debug("Message received: {}", message);
    }

    @Override
    public void onSessionUp(final BGPSession session) {
        LOG.debug("Session Up");
        this.session = session;
        this.up = true;
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.debug("Session Down", e);
        this.down = true;
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause.toString());
    }

    @Override
    public void releaseConnection() {
        LOG.debug("Releasing connection");
        if (this.session != null) {
            try {
                this.session.close();
            } catch (final Exception e) {
                LOG.warn("Error closing session", e);
            }
            this.session = null;
        }
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
