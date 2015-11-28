/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
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
import org.opendaylight.tcpmd5.api.KeyMapping;

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
    private final int disconnectAfter;
    private final int reconnectAfter;
    private final PCEPCapability pcepCapabilities;
    private final Optional<BigInteger> syncOptDBVersion;
    private PCCDispatcherImpl pccDispatcher;
    private Timer timer = new HashedWheelTimer();
    private boolean reconnectActive = false;

    public PCCsBuilder(final int lsps, final boolean pcError, final int pccCount, @Nonnull final InetSocketAddress localAddress,
                       @Nonnull final List<InetSocketAddress> remoteAddress, final short keepAlive, final short deadTimer, @Nonnull final String password,
                       final long reconnectTime, final int redelegationTimeout, final int stateTimeout, final Optional<BigInteger> syncOptDBVersion,
                       @Nonnull final PCEPCapability pcepCapabilities, final int disconnectAfter, final int reconnectAfter) {
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
        this.syncOptDBVersion = syncOptDBVersion;
        this.disconnectAfter = disconnectAfter;
        this.reconnectAfter = reconnectAfter;
        this.pcepCapabilities = pcepCapabilities;
        startActivators();
    }

    final class ReconnectTask implements TimerTask {
        @Override
        public void run(final Timeout timeout) throws Exception {
            createPCCs(syncOptDBVersion.isPresent() ? syncOptDBVersion.get(): BigInteger.ONE);
        }
    }

    final class DisconnectTask implements TimerTask {
        @Override
        public void run(final Timeout timeout) throws Exception {
            pccDispatcher.close();
            if (reconnectAfter > 0) {
                timer.newTimeout(new ReconnectTask(), reconnectAfter, TimeUnit.SECONDS);
            }
        }
    }

    private void createDisconnectTask() {
        if (this.disconnectAfter > 0) {
            this.timer.newTimeout(new DisconnectTask(), this.disconnectAfter, TimeUnit.SECONDS);
            reconnectActive = true;
        }
    }

    void createPCCs(final BigInteger initialDBVersion) throws InterruptedException, ExecutionException {
        InetAddress currentAddress = this.localAddress.getAddress();
        this.pccDispatcher = new PCCDispatcherImpl(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry());
        for (int i = 0; i < this.pccCount; i++) {
            final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(this.lsps, currentAddress, this.redelegationTimeout, this.stateTimeout, this.timer);
            createPCC(new InetSocketAddress(currentAddress, localAddress.getPort()), tunnelManager, initialDBVersion);
            currentAddress = InetAddresses.increment(currentAddress);
        }
        if (syncOptDBVersion.isPresent() && !reconnectActive) {
            createDisconnectTask();
        }
    }

    private void createPCC(@Nonnull final InetSocketAddress localAddress, @Nonnull final PCCTunnelManager tunnelManager,
                           final BigInteger initialDBVersion) throws InterruptedException, ExecutionException {

        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        for (final InetSocketAddress pceAddress : this.remoteAddress) {
            this.pccDispatcher.createClient(pceAddress, this.reconnectTime,
                new PCEPSessionListenerFactory() {
                    @Override
                    public PCEPSessionListener getSessionListener() {
                        return new PCCSessionListener(remoteAddress.indexOf(pceAddress), tunnelManager, pcError);
                    }
                }, snf, getKeyMapping(pceAddress.getAddress(), password), localAddress, initialDBVersion);
        }
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        final List<PCEPCapability> capabilities = Lists.newArrayList(this.pcepCapabilities);
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(this.deadTimer, this.keepAlive, capabilities), 0);
    }

    private static KeyMapping getKeyMapping(@Nonnull final InetAddress inetAddress, @Nullable final String password) {
        if (!isNullOrEmpty(password)) {
            final KeyMapping keyMapping = new KeyMapping();
            keyMapping.put(inetAddress, password.getBytes(Charsets.US_ASCII));
            return keyMapping;
        }
        return null;
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
