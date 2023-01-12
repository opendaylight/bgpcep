/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static java.util.Objects.requireNonNull;

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
import org.opendaylight.protocol.pcep.pcc.mock.protocol.MockPcepSessionErrorPolicy;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCDispatcherImpl;
import org.opendaylight.protocol.pcep.pcc.mock.protocol.PCCSessionListener;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.DefaultPCEPExtensionConsumerContext;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

final class PCCsBuilder {
    private final int lsps;
    private final boolean pcError;
    private final int pccCount;
    private final InetSocketAddress localAddress;
    private final List<InetSocketAddress> remoteAddress;
    private final Uint8 keepAlive;
    private final Uint8 deadTimer;
    private final String password;
    private final long reconnectTime;
    private final int redelegationTimeout;
    private final int stateTimeout;
    private final PCEPCapability pcepCapabilities;
    private final Timer timer = new HashedWheelTimer();
    private final MessageRegistry registry;

    PCCsBuilder(final int lsps, final boolean pcError, final int pccCount,
            final @NonNull InetSocketAddress localAddress, final @NonNull List<InetSocketAddress> remoteAddress,
            final @NonNull Uint8 keepAlive, final @NonNull Uint8 deadTimer, final @Nullable String password,
            final long reconnectTime, final int redelegationTimeout, final int stateTimeout,
            final @NonNull PCEPCapability pcepCapabilities) {
        this.lsps = lsps;
        this.pcError = pcError;
        this.pccCount = pccCount;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.keepAlive = requireNonNull(keepAlive);
        this.deadTimer = requireNonNull(deadTimer);
        this.password = password;
        this.reconnectTime = reconnectTime;
        this.redelegationTimeout = redelegationTimeout;
        this.stateTimeout = stateTimeout;
        this.pcepCapabilities = pcepCapabilities;

        registry = new DefaultPCEPExtensionConsumerContext().getMessageHandlerRegistry();
    }

    void createPCCs(final Uint64 initialDBVersion, final Optional<TimerHandler> timerHandler) {
        InetAddress currentAddress = localAddress.getAddress();
        PCCDispatcherImpl pccDispatcher = new PCCDispatcherImpl(registry);

        if (timerHandler.isPresent()) {
            timerHandler.get().setPCCDispatcher(pccDispatcher);
        }
        for (int i = 0; i < pccCount; i++) {
            final PCCTunnelManager tunnelManager = new PCCTunnelManagerImpl(lsps, currentAddress,
                redelegationTimeout, stateTimeout, timer, timerHandler);
            createPCC(pccDispatcher, new InetSocketAddress(currentAddress, localAddress.getPort()), tunnelManager,
                initialDBVersion);
            currentAddress = InetAddresses.increment(currentAddress);
        }
    }

    private void createPCC(final PCCDispatcherImpl pccDispatcher, final @NonNull InetSocketAddress plocalAddress,
            final PCCTunnelManager tunnelManager, final Uint64 initialDBVersion) {
        final PCEPSessionNegotiatorFactory<PCEPSessionImpl> snf = getSessionNegotiatorFactory();
        for (final InetSocketAddress pceAddress : remoteAddress) {
            pccDispatcher.createClient(pceAddress, reconnectTime,
                () -> new PCCSessionListener(remoteAddress.indexOf(pceAddress), tunnelManager, pcError), snf,
                    password == null ? KeyMapping.of() : KeyMapping.of(pceAddress.getAddress(), password),
                        plocalAddress, initialDBVersion);
        }
    }

    private PCEPSessionNegotiatorFactory<PCEPSessionImpl> getSessionNegotiatorFactory() {
        final List<PCEPCapability> capabilities = Lists.newArrayList(pcepCapabilities);
        return new DefaultPCEPSessionNegotiatorFactory(new BasePCEPSessionProposalFactory(deadTimer,
            keepAlive, capabilities), MockPcepSessionErrorPolicy.ZERO);
    }
}
