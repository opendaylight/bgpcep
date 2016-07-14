package org.opendaylight.protocol.pcep.pcc.mock;

import java.net.InetAddress;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;

interface TestListenerFactory extends PCEPSessionListenerFactory {
    TestingSessionListener getSessionListenerByRemoteAddress(final InetAddress ipAddress) throws InterruptedException;
}
