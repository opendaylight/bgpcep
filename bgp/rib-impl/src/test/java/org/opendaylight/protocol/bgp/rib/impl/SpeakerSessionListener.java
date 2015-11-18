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
import java.util.Set;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the BGP Speaker.
 */
public class SpeakerSessionListener implements BGPSessionListener {

    public List<Notification> messages = Lists.newArrayList();

    public boolean up = false;

    public Set<BgpTableType> types;

    private static final Logger LOG = LoggerFactory.getLogger(SpeakerSessionListener.class);

    public SpeakerSessionListener() {
    }

    @Override
    public void onMessage(final BGPSession session, final Notification message) {
        LOG.debug("Received message: {} {}", message.getClass(), message);
        this.messages.add(message);
    }

    @Override
    public void releaseConnection() {

    }

    @Override
    public synchronized void onSessionUp(final BGPSession session) {
        LOG.debug("Session up.");
        this.up = true;
        this.types = session.getAdvertisedTableTypes();
        this.notifyAll();
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.debug("Session down.");
        this.up = false;
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause.toString());
        this.up = false;
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
