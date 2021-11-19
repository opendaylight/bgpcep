/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPDispatcherDependencies;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

public final class TestToolPCEPDispatcherDependencies implements PCEPDispatcherDependencies {
    private final @NonNull PCEPSessionListenerFactory listenerFactory = new TestingSessionListenerFactory();
    private final InetSocketAddress address;

    TestToolPCEPDispatcherDependencies(final InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public KeyMapping getKeys() {
        return KeyMapping.of();
    }

    @Override
    public PCEPSessionListenerFactory getListenerFactory() {
        return listenerFactory;
    }
}
