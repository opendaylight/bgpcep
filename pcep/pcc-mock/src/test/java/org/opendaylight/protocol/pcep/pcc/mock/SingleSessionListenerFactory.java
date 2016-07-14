package org.opendaylight.protocol.pcep.pcc.mock;

import java.net.InetAddress;
import org.opendaylight.protocol.pcep.PCEPSessionListener;


class SingleSessionListenerFactory implements TestListenerFactory {
    private final TestingSessionListener sessionListener;

    SingleSessionListenerFactory() {
        this.sessionListener = new TestingSessionListener();
    }

    @Override
    public PCEPSessionListener getSessionListener() {
        return this.sessionListener;
    }

    @Override
    public TestingSessionListener getSessionListenerByRemoteAddress(final InetAddress ipAddress) throws InterruptedException {
        return this.sessionListener;
    }
}
