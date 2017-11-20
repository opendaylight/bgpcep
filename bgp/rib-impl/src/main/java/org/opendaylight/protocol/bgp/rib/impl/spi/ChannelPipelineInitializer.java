/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.annotations.Beta;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;

@Beta
public interface ChannelPipelineInitializer<S extends BGPSession> {
    void initializeChannel(SocketChannel socketChannel, Promise<S> promise);
}
