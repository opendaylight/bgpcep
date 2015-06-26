package org.opendaylight.protocol.pcep;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

/**
 * Created by cgasparini on 25.6.2015.
 */
public interface PCEPSessionNegotiatorFactory {
    SessionNegotiator getSessionNegotiator(PCEPSessionListenerFactory sessionListenerFactory, Channel channel, Promise<PCEPSession> promise);
}
