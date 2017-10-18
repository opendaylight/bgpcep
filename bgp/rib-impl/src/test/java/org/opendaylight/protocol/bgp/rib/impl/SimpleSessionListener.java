/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.junit.Assert;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.util.CheckUtil.ListenerCheck;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the client.
 */
public final class SimpleSessionListener implements BGPSessionListener, ListenerCheck {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);
    @GuardedBy("this")
    private final List<Notification> listMsg = Lists.newArrayList();
    private BGPSession session;
    private final CountDownLatch sessionLatch = new CountDownLatch(1);

    public SimpleSessionListener() {
    }

    synchronized List<Notification> getListMsg() {
        return this.listMsg;
    }

    @Override
    public void markUptodate(final TablesKey tablesKey) {
        LOG.debug("Table marked as up-to-date {}", tablesKey);
    }

    @Override
    public void onSessionUp(final BGPSession session) {
        LOG.info("Session Up");
        this.session = session;
        this.sessionLatch.countDown();
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.debug("Session Down", e);
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause.toString());
    }

    @Override
    public synchronized void onMessage(final BGPSession session, final Notification message) {
        this.listMsg.add(message);
        LOG.debug("Message received: {}", message);
    }

    @Override
    public ListenableFuture<Void> releaseConnection() {
        LOG.debug("Releasing connection");
        if (this.session != null) {
            try {
                this.session.close();
            } catch (final Exception e) {
                LOG.warn("Error closing session", e);
            }
        }
        return Futures.immediateFuture(null);
    }

    public State getState() {
        return getSession().getState();
    }

    BGPSessionImpl getSession() {
        Assert.assertEquals("Session up", true, Uninterruptibles.awaitUninterruptibly(this.sessionLatch, 10, TimeUnit.SECONDS));
        return (BGPSessionImpl) this.session;
    }

    @Override
    public int getListMessageSize() {
        return this.listMsg.size();
    }
}
