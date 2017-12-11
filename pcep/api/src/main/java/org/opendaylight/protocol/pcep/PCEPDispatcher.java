/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.channel.ChannelFuture;
import javax.annotation.Nonnull;

/**
 * Dispatcher class for creating servers and clients.
 */
public interface PCEPDispatcher {
    /**
     * Creates server. Each server needs three factories to pass their instances to client sessions.
     *
     * @param dispatcherDependencies contains required dependencies for instantiate a PCEP Server
     * @return instance of PCEPServer
     */
    @Nonnull ChannelFuture createServer(@Nonnull PCEPDispatcherDependencies dispatcherDependencies);

    @Nonnull PCEPSessionNegotiatorFactory<?> getPCEPSessionNegotiatorFactory();
}
