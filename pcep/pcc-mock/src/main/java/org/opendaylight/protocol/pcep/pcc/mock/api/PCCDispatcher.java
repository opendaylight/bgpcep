/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.api;

import io.netty.util.concurrent.Future;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;

public interface PCCDispatcher {

    @Nonnull
    Future<PCEPSession> createClient(@Nonnull InetSocketAddress remoteAddress,
            long reconnectTime, @Nonnull PCEPSessionListenerFactory listenerFactory,
            @Nonnull PCEPSessionNegotiatorFactory<? extends PCEPSession> negotiatorFactory, @Nonnull KeyMapping keys,
            @Nonnull InetSocketAddress localAddress, @Nonnull BigInteger dbVersion);

    @Nonnull
    Future<PCEPSession> createClient(@Nonnull InetSocketAddress remoteAddress,
            long reconnectTime, @Nonnull PCEPSessionListenerFactory listenerFactory,
            @Nonnull PCEPSessionNegotiatorFactory<? extends PCEPSession> negotiatorFactory, @Nonnull KeyMapping keys,
            @Nonnull InetSocketAddress localAddress);
}
