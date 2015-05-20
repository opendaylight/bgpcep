package org.opendaylight.protocol.bmp.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class RouterSessionManager implements SessionListenerFactory<BmpSessionListener>, AutoCloseable {

    public RouterSessionManager(final YangInstanceIdentifier yangMonitorId, final DOMDataBroker domDataBroker) {
    }

    @Override
    public BmpSessionListener getSessionListener() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

}
