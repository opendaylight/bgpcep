package org.opendaylight.protocol.bgp.rib.impl.protocol;

import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;

/**
 * Created by cgasparini on 25.6.2015.
 */
public interface BGPSessionListenerFactory {
    BGPSessionListener getSessionListener();
}