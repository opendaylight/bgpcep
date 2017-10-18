/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing BGP Listener.
 */
final class TestingListener implements BGPSessionListener {
    private static final Logger LOG = LoggerFactory.getLogger(TestingListener.class);
    private final int nPrefixes;
    private final List<String> extCom;
    private final boolean multiPathSupport;
    private LongAdder messageCounter = new LongAdder();

    TestingListener(final int nPrefixes, final List<String> extCom, final boolean multiPathSupport) {
        this.nPrefixes = nPrefixes;
        this.extCom = extCom;
        this.multiPathSupport = multiPathSupport;
    }

    @Override
    public void markUptodate(final TablesKey tablesKey) {
        LOG.debug("Table marked as up-to-date {}", tablesKey);
    }

    @Override
    public void onSessionUp(final BGPSession session) {
        LOG.info("Client Listener: Session Up.");
        if (this.nPrefixes > 0) {
            PrefixesBuilder.advertiseIpv4Prefixes(((BGPSessionImpl) session).getLimiter(), this.nPrefixes, this.extCom, this.multiPathSupport);
        }
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
    public void onMessage(final BGPSession session, final Notification message) {
        if (message instanceof Update) {
            this.messageCounter.increment();
        }
        LOG.debug("Message received: {}", message.toString());
    }

    @Override
    public ListenableFuture<?> releaseConnection() {
        LOG.info("Client Listener: Connection released.");
        return Futures.immediateFuture(null);
    }

    void printCount(final String localAddress) {
        LOG.info("Peer {} received {} update messages.", localAddress, this.messageCounter.longValue());
    }
}
