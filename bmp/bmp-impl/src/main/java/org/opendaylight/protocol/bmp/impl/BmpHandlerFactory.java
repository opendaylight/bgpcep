/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl;

import static java.util.Objects.requireNonNull;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandler;

import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;

public final class BmpHandlerFactory {
    private final ChannelOutboundHandler encoder;
    private final BmpMessageRegistry registry;

    public BmpHandlerFactory(final BmpMessageRegistry registry) {
        this.registry = requireNonNull(registry);
        this.encoder = new BmpMessageToByteEncoder(registry);
    }

    public ChannelHandler[] getEncoders() {
        return new ChannelHandler[]{this.encoder,};
    }

    public ChannelHandler[] getDecoders() {
        return new ChannelHandler[]{new BmpMessageHeaderDecoder(), new BmpByteToMessageDecoder(this.registry),};
    }

}
