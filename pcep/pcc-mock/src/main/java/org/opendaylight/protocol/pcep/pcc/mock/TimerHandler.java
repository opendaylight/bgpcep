/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Optional;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TimerHandler.class);
    private final int disconnectAfter;
    private final Timer timer = new HashedWheelTimer();
    private PCCDispatcherImpl pccDispatcher;
    private final int reconnectAfter;
    private final Optional<BigInteger> syncOptDBVersion;
    private final PCCsBuilder pcCsBuilder;
    private boolean reconnectActive = false;

    public TimerHandler(final PCCsBuilder pcCsBuilder, final Optional<BigInteger> syncOptDBVersion,
            final int disconnectAfter, final int reconnectAfter) {
        this.pcCsBuilder = pcCsBuilder;
        this.syncOptDBVersion = syncOptDBVersion;
        this.disconnectAfter = disconnectAfter;
        this.reconnectAfter = reconnectAfter;
    }

    final class DisconnectTask implements TimerTask {
        @Override
        public void run(final Timeout timeout) throws Exception {
            LOG.debug("Disconnects PCCs, reconnect after {} seconds", TimerHandler.this.reconnectAfter);
            TimerHandler.this.pccDispatcher.close();
            if (TimerHandler.this.reconnectAfter > 0) {
                TimerHandler.this.timer.newTimeout(new ReconnectTask(),
                        TimerHandler.this.reconnectAfter, TimeUnit.SECONDS);
            }
        }
    }

    final class ReconnectTask implements TimerTask {
        @Override
        public void run(final Timeout timeout) throws Exception {
            LOG.debug("Reconnecting PCCs}");
            TimerHandler.this.pcCsBuilder.createPCCs(TimerHandler.this.syncOptDBVersion.isPresent()
                    ? TimerHandler.this.syncOptDBVersion.get() : BigInteger.ONE, Optional.absent());
        }
    }

    protected void createDisconnectTask() {
        if (this.disconnectAfter > 0 && !this.reconnectActive && this.syncOptDBVersion.isPresent()) {
            this.timer.newTimeout(new DisconnectTask(), this.disconnectAfter, TimeUnit.SECONDS);
            this.reconnectActive = true;
        }
    }

    public void setPCCDispatcher(final PCCDispatcherImpl pccDispatcher) {
        this.pccDispatcher = pccDispatcher;
    }
}
