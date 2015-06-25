package org.opendaylight.protocol.bgp.rib.impl.protocol;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;

/**
 * Created by cgasparini on 25.6.2015.
 */
public interface BGPSessionNegotiatorFactory {
    SessionNegotiator getSessionNegotiator(BGPSessionListenerFactory factory, Channel channel, Promise<BGPSession> promise);
}