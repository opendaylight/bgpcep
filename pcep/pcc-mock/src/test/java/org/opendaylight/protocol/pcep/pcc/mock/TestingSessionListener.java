/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.junit.Assert;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.util.CheckTestUtil.ListenerCheck;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestingSessionListener implements PCEPSessionListener, ListenerCheck {

    private static final Logger LOG = LoggerFactory.getLogger(TestingSessionListener.class);
    private final CountDownLatch sessionLatch = new CountDownLatch(1);

    @GuardedBy("this")
    private final List<Message> messages = new ArrayList<>();

    private boolean up = false;
    private PCEPSession session = null;

    @Override
    public synchronized void onMessage(final PCEPSession psession, final Message message) {
        LOG.debug("Received message: {}", message);
        this.messages.add(message);
    }

    @Override
    public void onSessionUp(final PCEPSession psession) {
        LOG.debug("Session up.");
        this.up = true;
        this.session = psession;
        this.sessionLatch.countDown();

    }

    @Override
    public void onSessionDown(final PCEPSession psession, final Exception exception) {
        LOG.debug("Session down. Cause : {} ", exception, exception);
        this.up = false;
        this.session = null;
    }

    @Override
    public void onSessionTerminated(final PCEPSession psession, final PCEPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause);
    }

    synchronized List<Message> messages() {
        return ImmutableList.copyOf(this.messages);
    }

    boolean isUp() {
        return this.up;
    }

    public PCEPSession getSession() {
        Assert.assertTrue("Session up", Uninterruptibles.awaitUninterruptibly(this.sessionLatch, 10, TimeUnit.SECONDS));
        return this.session;
    }

    @Override
    public synchronized int getListMessageSize() {
        return this.messages.size();
    }
}
