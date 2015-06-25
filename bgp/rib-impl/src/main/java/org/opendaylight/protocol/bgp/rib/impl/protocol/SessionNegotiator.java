package org.opendaylight.protocol.bgp.rib.impl.protocol;

import io.netty.channel.ChannelInboundHandler;
import java.io.Closeable;

/**
 * Created by cgasparini on 25.6.2015.
 */
public interface SessionNegotiator<T extends Closeable> extends ChannelInboundHandler {
}
