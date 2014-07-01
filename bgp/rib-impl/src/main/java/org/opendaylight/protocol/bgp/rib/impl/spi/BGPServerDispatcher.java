/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;

/**
 * Dispatcher class for creating BGP server.
 */
public interface BGPServerDispatcher {

    ChannelFuture createServer(InetSocketAddress address, BGPSessionPreferences preferences, RIB rib, BGPSessionValidator sessionValidator);

    /**
     * Create new BGP server bound to provided socket address.
     */
    ChannelFuture createServer(InetSocketAddress address, BGPSessionPreferences preferences, RIB rib, BGPSessionValidator sessionValidator, KeyMapping keys);
}
