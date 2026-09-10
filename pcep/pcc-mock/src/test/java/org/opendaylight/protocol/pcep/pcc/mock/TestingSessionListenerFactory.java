/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.net.InetAddress;
import java.util.ArrayList;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

class TestingSessionListenerFactory implements PCEPSessionListenerFactory {
    @GuardedBy("this")
    private final ArrayList<TestingSessionListener> sessionListeners = new ArrayList<>();

    @Override
    public PCEPSessionListener getSessionListener() {
        final var sessionListener = new TestingSessionListener();
        sessionListeners.add(sessionListener);
        return sessionListener;
    }

    TestingSessionListener getSessionListenerByRemoteAddress(final InetAddress ipAddress) {
        for (var sessionListener : sessionListeners) {
            if (sessionListener.isUp()) {
                final var session = sessionListener.getSession();
                if (session.getRemoteAddress().equals(ipAddress)) {
                    return sessionListener;
                }
            }
        }
        return null;
    }
}
