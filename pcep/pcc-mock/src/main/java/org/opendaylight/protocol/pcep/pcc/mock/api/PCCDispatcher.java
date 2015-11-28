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
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;

public interface PCCDispatcher {

    Future<PCEPSession> createClient(final InetSocketAddress remoteAddress,
            final long reconnectTime, final PCEPSessionListenerFactory listenerFactory,
            final PCEPSessionNegotiatorFactory<? extends PCEPSession> negotiatorFactory, final KeyMapping keys,
            final InetSocketAddress localAddress, final BigInteger dbVersion);

    Future<PCEPSession> createClient(final InetSocketAddress remoteAddress,
                                     final long reconnectTime, final PCEPSessionListenerFactory listenerFactory,
                                     final PCEPSessionNegotiatorFactory<? extends PCEPSession> negotiatorFactory, final KeyMapping keys,
                                     final InetSocketAddress localAddress);
}
