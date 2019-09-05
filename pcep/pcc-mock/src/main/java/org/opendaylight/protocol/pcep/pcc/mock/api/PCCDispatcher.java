/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock.api;

import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.yangtools.yang.common.Uint64;

public interface PCCDispatcher {

    @NonNull Future<PCEPSession> createClient(@NonNull InetSocketAddress remoteAddress,
            long reconnectTime, @NonNull PCEPSessionListenerFactory listenerFactory,
            @NonNull PCEPSessionNegotiatorFactory<? extends PCEPSession> negotiatorFactory, @NonNull KeyMapping keys,
            @NonNull InetSocketAddress localAddress, @NonNull Uint64 dbVersion);

    @NonNull Future<PCEPSession> createClient(@NonNull InetSocketAddress remoteAddress,
            long reconnectTime, @NonNull PCEPSessionListenerFactory listenerFactory,
            @NonNull PCEPSessionNegotiatorFactory<? extends PCEPSession> negotiatorFactory, @NonNull KeyMapping keys,
            @NonNull InetSocketAddress localAddress);
}
