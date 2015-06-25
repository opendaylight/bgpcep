package org.opendaylight.protocol.bgp.rib.impl.protocol;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;

/**
 * Created by cgasparini on 25.6.2015.
 */
public interface BGPSessionNegotiatorFactory {
    SessionNegotiator<BGPSessionImpl> getSessionNegotiator(BGPSessionListenerFactory var1, Channel var2, Promise<BGPSessionImpl> var3);
}