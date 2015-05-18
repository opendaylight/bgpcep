package org.opendaylight.protocol.bmp.impl;

import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandler;

import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;

public class BmpHandlerFactory {
    private final ChannelOutboundHandler encoder;
    private final BmpMessageRegistry registry;

    public BmpHandlerFactory(final BmpMessageRegistry registry) {
        this.registry = Preconditions.checkNotNull(registry);
        this.encoder = new BmpMessageToByteEncoder(registry);
    }

    public ChannelHandler[] getEncoders() {
        return new ChannelHandler[]{this.encoder,};
    }

    public ChannelHandler[] getDecoders() {
        return new ChannelHandler[] { new BmpMessageHeaderDecoder(), new BmpByteToMessageDecoder(this.registry), };
    }

}
