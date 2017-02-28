/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

class TestingSessionListenerFactory implements PCEPSessionListenerFactory {

    @GuardedBy("this")
    private final List<TestingSessionListener> sessionListeners = new ArrayList<>();

    @Override
    public PCEPSessionListener getSessionListener() {
        final TestingSessionListener sessionListener = new TestingSessionListener();
        this.sessionListeners.add(sessionListener);
        return sessionListener;
    }

    TestingSessionListener getSessionListenerByRemoteAddress(final InetAddress ipAddress) {
        for (final TestingSessionListener sessionListener : this.sessionListeners) {
            if (sessionListener.isUp()) {
                final PCEPSession session = sessionListener.getSession();
                if (session.getRemoteAddress().equals(ipAddress)) {
                    return sessionListener;
                }
            }
        }
        return null;
    }
}
