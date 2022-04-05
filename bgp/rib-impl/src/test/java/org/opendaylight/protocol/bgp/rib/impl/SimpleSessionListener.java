/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.util.CheckUtil.ListenerCheck;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the client.
 */
public final class SimpleSessionListener implements BGPSessionListener, ListenerCheck {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);

    @GuardedBy("this")
    private final List<Notification<?>> listMsg = new ArrayList<>();
    private final CountDownLatch sessionLatch = new CountDownLatch(1);

    private BGPSession bgpSession;

    @Override
    public void markUptodate(final TablesKey tablesKey) {
        LOG.debug("Table marked as up-to-date {}", tablesKey);
    }

    @Override
    public void onSessionUp(final BGPSession session) {
        LOG.info("Session Up");
        bgpSession = session;
        sessionLatch.countDown();
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception exception) {
        LOG.debug("Session Down", exception);
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause.toString());
    }

    @Override
    public synchronized void onMessage(final BGPSession session, final Notification<?> message) {
        listMsg.add(message);
        LOG.debug("Message received: {}", message);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public ListenableFuture<Void> releaseConnection() {
        LOG.debug("Releasing connection");
        if (bgpSession != null) {
            try {
                bgpSession.close();
            } catch (final Exception e) {
                LOG.warn("Error closing session", e);
            }
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<?> restartGracefully(final long selectionDeferralTimerSeconds) {
        return Futures.immediateFailedFuture(
                new UnsupportedOperationException("SimpleSessionListener doesn't support graceful restart"));
    }

    public State getState() {
        return getSession().getState();
    }

    BGPSessionImpl getSession() {
        assertTrue("Session up",
                Uninterruptibles.awaitUninterruptibly(sessionLatch, 10, TimeUnit.SECONDS));
        return (BGPSessionImpl) bgpSession;
    }

    @Override
    public synchronized List<Notification<?>> getListMsg() {
        return listMsg;
    }

    @Override
    public int getListMessageSize() {
        return listMsg.size();
    }
}
