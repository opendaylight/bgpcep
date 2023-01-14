/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;

public final class TestToolPCEPNegotiatorDependencies implements PCEPSessionNegotiatorFactoryDependencies {
    private final @NonNull PCEPSessionListenerFactory listenerFactory = new TestingSessionListenerFactory();

    @Override
    public PCEPSessionListenerFactory getListenerFactory() {
        return listenerFactory;
    }
}
