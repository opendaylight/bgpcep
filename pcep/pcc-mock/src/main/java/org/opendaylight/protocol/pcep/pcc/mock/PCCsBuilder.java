/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCTunnelManager;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.yangtools.yang.common.Uint64;

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
    private final MessageRegistry registry;

    private PCCDispatcherImpl pccDispatcher;

    PCCsBuilder(final int lsps, final boolean pcError, final int pccCount,
            final @NonNull InetSocketAddress localAddress, final @NonNull List<InetSocketAddress> remoteAddress,
            final short keepAlive, final short deadTimer, final @Nullable String password, final long reconnectTime,
            final int redelegationTimeout, final int stateTimeout, final @NonNull PCEPCapability pcepCapabilities) {
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

//        final PCEPExtensionProviderContext ctx = new SimplePCEPExtensionProviderContext();
//        new PCCActivator().start(ctx);
//        new StatefulActivator().start(ctx);
//        new SyncOptimizationsActivator().start(ctx);
//        new InitiatedActivator().start(ctx);

        this.registry = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();
    }

    void createPCCs(final Uint64 initialDBVersion, final Optional<TimerHandler> timerHandler) {
        InetAddress currentAddress = this.localAddress.getAddress();
        this.pccDispatcher = new PCCDispatcherImpl(registry);
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

    private void createPCC(final @NonNull InetSocketAddress plocalAddress,
            final PCCTunnelManager tunnelManager, final Uint64 initialDBVersion) {
        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        for (final InetSocketAddress pceAddress : this.remoteAddress) {
            this.pccDispatcher.createClient(pceAddress, this.reconnectTime, () -> new PCCSessionListener(
                            this.remoteAddress.indexOf(pceAddress), tunnelManager, this.pcError), snf,
                    KeyMapping.getKeyMapping(pceAddress.getAddress(), this.password), plocalAddress, initialDBVersion);
        }
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        final List<PCEPCapability> capabilities = Lists.newArrayList(this.pcepCapabilities);
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(this.deadTimer,
            this.keepAlive, capabilities), 0);
    }
}
