package org.opendaylight.protocol.bmp.impl;

import com.google.common.base.Preconditions;

import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

final class RouterSessionManager implements SessionListenerFactory<BmpSessionListener>, AutoCloseable {

    private final DOMDataBroker domDataBroker;
    private final YangInstanceIdentifier yangMonitorId;

    public RouterSessionManager(final YangInstanceIdentifier yangMonitorId, final DOMDataBroker domDataBroker) {
        // TODO Auto-generated method stub
        this.domDataBroker = Preconditions.checkNotNull(domDataBroker);
        this.yangMonitorId = yangMonitorId;

    }

    @Override
    public BmpSessionListener getSessionListener() {
        return this.getSessionListener();
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }


}
