/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.ietf.initiated00.CrabbeInitiatedActivator;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;

final class PCCsBuilder {
    private final int lsps;
    private final boolean pcError;
    private final int pccCount;
    private final InetSocketAddress localAddress;
    private final List<InetSocketAddress> remoteAddress;
    private final short keepAlive;
    private final short deadTimer;
    private final String password;
    private final long reconnectTime;
    private final int redelegationTimeout;
    private final int stateTimeout;
    private final PCEPCapability pcepCapabilities;
    private final Timer timer = new HashedWheelTimer();
    private PCCDispatcherImpl pccDispatcher;

    PCCsBuilder(final int lsps, final boolean pcError, final int pccCount,
            @Nonnull final InetSocketAddress localAddress, @Nonnull final List<InetSocketAddress> remoteAddress,
            final short keepAlive, final short deadTimer, @Nullable final String password, final long reconnectTime,
            final int redelegationTimeout, final int stateTimeout, @Nonnull final PCEPCapability pcepCapabilities) {
        this.lsps = lsps;
        this.pcError = pcError;
        this.pccCount = pccCount;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.keepAlive = keepAlive;
        this.deadTimer = deadTimer;
        this.password = password;
        this.reconnectTime = reconnectTime;
        this.redelegationTimeout = redelegationTimeout;
        this.stateTimeout = stateTimeout;
        this.pcepCapabilities = pcepCapabilities;
        startActivators();
    }

    void createPCCs(final BigInteger initialDBVersion, final Optional<TimerHandler> timerHandler)
        throws InterruptedException, ExecutionException {
        InetAddress currentAddress = this.localAddress.getAddress();
        this.pccDispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getMessageHandlerRegistry());
        if (timerHandler.isPresent()) {
            timerHandler.get().setPCCDispatcher(this.pccDispatcher);
        }
        for (int i = 0; i < this.pccCount; i++) {
            final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(this.lsps, currentAddress,
                this.redelegationTimeout, this.stateTimeout, this.timer, timerHandler);
            createPCC(new InetSocketAddress(currentAddress, this.localAddress.getPort()), tunnelManager,
                    initialDBVersion);
            currentAddress = InetAddresses.increment(currentAddress);
        }
    }

    private void createPCC(@Nonnull final InetSocketAddress localAddress, @Nonnull final PCCTunnelManager tunnelManager,
        final BigInteger initialDBVersion) throws InterruptedException, ExecutionException {
        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        for (final InetSocketAddress pceAddress : this.remoteAddress) {
            this.pccDispatcher.createClient(pceAddress, this.reconnectTime, () -> new PCCSessionListener(
                    this.remoteAddress.indexOf(pceAddress), tunnelManager, this.pcError), snf,
                KeyMapping.getKeyMapping(pceAddress.getAddress(), this.password), localAddress, initialDBVersion);
        }
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        final List<PCEPCapability> capabilities = Lists.newArrayList(this.pcepCapabilities);
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(this.deadTimer,
            this.keepAlive, capabilities), 0);
    }

    private static void startActivators() {
        final PCCActivator pccActivator = new PCCActivator();
        final StatefulActivator stateful = new StatefulActivator();
        final SyncOptimizationsActivator optimizationsActivator = new SyncOptimizationsActivator();
        final CrabbeInitiatedActivator activator = new CrabbeInitiatedActivator();
        final PCEPExtensionProviderContext ctx = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance();
        pccActivator.start(ctx);
        stateful.start(ctx);
        optimizationsActivator.start(ctx);
        activator.start(ctx);
    }
}
