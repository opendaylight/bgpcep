/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TimerHandler.class);

    private final Timer timer = new HashedWheelTimer();
    private final int disconnectAfter;
    private final int reconnectAfter;
    private final Optional<Uint64> syncOptDBVersion;
    private final PCCsBuilder pcCsBuilder;

    private PCCDispatcherImpl pccDispatcher = null;
    private boolean reconnectActive = false;

    public TimerHandler(final PCCsBuilder pcCsBuilder, final Optional<Uint64> syncOptDBVersion,
            final int disconnectAfter, final int reconnectAfter) {
        this.pcCsBuilder = pcCsBuilder;
        this.syncOptDBVersion = syncOptDBVersion;
        this.disconnectAfter = disconnectAfter;
        this.reconnectAfter = reconnectAfter;
    }

    final class DisconnectTask implements TimerTask {
        @Override
        public void run(final Timeout timeout) {
            LOG.debug("Disconnects PCCs, reconnect after {} seconds", reconnectAfter);
            pccDispatcher.close();
            if (reconnectAfter > 0) {
                timer.newTimeout(new ReconnectTask(), reconnectAfter, TimeUnit.SECONDS);
            }
        }
    }

    final class ReconnectTask implements TimerTask {
        @Override
        public void run(final Timeout timeout) {
            LOG.debug("Reconnecting PCCs}");
            pcCsBuilder.createPCCs(syncOptDBVersion.orElse(Uint64.ONE), Optional.empty());
        }
    }

    protected void createDisconnectTask() {
        if (disconnectAfter > 0 && !reconnectActive && syncOptDBVersion.isPresent()) {
            timer.newTimeout(new DisconnectTask(), disconnectAfter, TimeUnit.SECONDS);
            reconnectActive = true;
        }
    }

    public void setPCCDispatcher(final PCCDispatcherImpl dispatcher) {
        pccDispatcher = dispatcher;
    }
}
