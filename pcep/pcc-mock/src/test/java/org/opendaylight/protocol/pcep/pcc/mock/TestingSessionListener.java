/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.util.CheckTestUtil.ListenerCheck;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TestingSessionListener implements PCEPSessionListener, ListenerCheck {
    private static final Logger LOG = LoggerFactory.getLogger(TestingSessionListener.class);

    private final CountDownLatch sessionLatch = new CountDownLatch(1);
    @GuardedBy("this")
    private final ArrayList<Message> messages = new ArrayList<>();

    private boolean up = false;
    private PCEPSession session = null;

    @Override
    public synchronized void onMessage(final PCEPSession psession, final Message message) {
        LOG.debug("Received message: {}", message);
        messages.add(message);
    }

    @Override
    public void onSessionUp(final PCEPSession psession) {
        LOG.debug("Session up.");
        up = true;
        session = psession;
        sessionLatch.countDown();
    }

    @Override
    public void onSessionDown(final PCEPSession psession, final Exception exception) {
        LOG.debug("Session down. Cause : {} ", exception, exception);
        up = false;
        session = null;
    }

    @Override
    public void onSessionTerminated(final PCEPSession psession, final PCEPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause);
    }

    synchronized List<Message> messages() {
        return ImmutableList.copyOf(messages);
    }

    boolean isUp() {
        return up;
    }

    PCEPSession getSession() {
        assertTrue(Uninterruptibles.awaitUninterruptibly(sessionLatch, 10, TimeUnit.SECONDS), "Session up");
        return session;
    }

    @Override
    public synchronized int getListMessageSize() {
        return messages.size();
    }
}
