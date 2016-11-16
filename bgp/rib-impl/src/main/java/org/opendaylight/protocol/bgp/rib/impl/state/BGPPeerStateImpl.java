/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStateListener;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BGPPeerStateImpl implements BGPPeerState,
    BGPSessionStateListener {
    private static final PortNumber NON_DEFINED_PORT = new PortNumber(0);
    private final Stopwatch sessionStopwatch;
    private final IpAddress neighborAddress;
    private final Set<TablesKey> afiSafisAdvertized;
    private final Set<TablesKey> afiSafisGracefulAdvertized;
    private final Set<TablesKey> afiSafisGracefulReceived = new HashSet<>();
    private final LongAdder updateSentCounter = new LongAdder();
    private final LongAdder notificationSentCounter = new LongAdder();
    private final LongAdder updateReceivedCounter = new LongAdder();
    private final LongAdder notificationReceivedCounter = new LongAdder();
    private final LongAdder erroneousUpdate = new LongAdder();
    private final Map<TablesKey, LongAdder> prefixesSent = new HashMap<>();
    private Map<TablesKey, LongAdder> prefixesReceived = Collections.emptyMap();
    private Map<TablesKey, LongAdder> prefixesInstalled = Collections.emptyMap();
    @GuardedBy("this")
    private State sessionState;
    private int holdTimerValue;
    private IpAddress remoteAddress;
    private PortNumber remotePort = NON_DEFINED_PORT;
    private PortNumber localPort = NON_DEFINED_PORT;
    private boolean localRestarting;
    private int peerRestartTime;
    private boolean peerRestarting;
    private boolean addPathCapability;
    private boolean asn32Capability;
    private boolean gracefulRestartCapability;
    private boolean multiProtocolCapability;
    private boolean routerRefreshCapability;

    public BGPPeerStateImpl(@Nonnull final IpAddress neighborAddress,
        @Nonnull final Set<TablesKey> afiSafisAdvertized,
        @Nonnull final Set<TablesKey> afiSafisGracefulAdvertized) {
        this.neighborAddress = Preconditions.checkNotNull(neighborAddress);
        this.sessionState = State.OPEN_CONFIRM;
        this.afiSafisAdvertized = Preconditions.checkNotNull(afiSafisAdvertized);
        this.afiSafisGracefulAdvertized = Preconditions.checkNotNull(afiSafisGracefulAdvertized);
        this.sessionStopwatch = Stopwatch.createUnstarted();
    }

    @Override
    public synchronized final void messageSent(final Notification msg) {
        if (this.sessionState == State.IDLE) {
            return;
        }
        if (msg instanceof Notify) {
            this.notificationSentCounter.increment();
        } else if (msg instanceof Update) {
            this.updateSentCounter.increment();
        }
    }

    @Override
    public  synchronized void messageReceived(final Notification msg) {
        if (this.sessionState == State.IDLE) {
            return;
        }
        if (msg instanceof Notify) {
            this.notificationReceivedCounter.increment();
        } else if (msg instanceof Update) {
            this.updateReceivedCounter.increment();
        }
    }

    @Override
    public final void advertizeCapabilities(final int holdTimerValue, final SocketAddress remoteAddress,
        final SocketAddress localAddress, final Set<BgpTableType> tableTypes, final List<BgpParameters> bgpParameters) {
        if (bgpParameters != null && !bgpParameters.isEmpty()) {
            for (final BgpParameters parameters : bgpParameters) {
                for (final OptionalCapabilities optionalCapabilities : parameters.getOptionalCapabilities()) {
                    final CParameters cParam = optionalCapabilities.getCParameters();
                    final CParameters1 capabilities = cParam.getAugmentation(CParameters1.class);
                    if (capabilities != null) {
                        final MultiprotocolCapability mc = capabilities.getMultiprotocolCapability();
                        if (mc != null) {
                            this.multiProtocolCapability = true;
                        }
                        if (capabilities.getGracefulRestartCapability() != null) {
                            this.gracefulRestartCapability = true;
                        }
                        if (capabilities.getAddPathCapability() != null) {
                            this.addPathCapability = true;
                        }
                        if (capabilities.getRouteRefreshCapability() != null) {
                            this.routerRefreshCapability = true;
                        }
                    }
                    if (cParam.getAs4BytesCapability() != null) {
                        this.asn32Capability = true;
                    }
                }
            }
        }

        this.holdTimerValue = holdTimerValue;
        this.remoteAddress = StrictBGPPeerRegistry.getIpAddress(remoteAddress);
        this.remotePort = new PortNumber(((InetSocketAddress) remoteAddress).getPort());
        this.localPort = new PortNumber(((InetSocketAddress) localAddress).getPort());
    }

    @Override
    public final synchronized void setSessionState(@Nonnull State state) {
        if (this.sessionState == State.IDLE) {
            this.sessionStopwatch.reset();
        } else if (this.sessionState == State.UP) {
            this.sessionStopwatch.start();
        }
        this.sessionState = state;
    }

    public final synchronized LongAdder getPrefixesInstalledCounter(final TablesKey tablesKey) {
        return this.prefixesInstalled.get(tablesKey);
    }

    @Override
    public final long getPrefixesSentCount(@Nonnull final TablesKey tablesKey) {
        return this.prefixesSent.get(tablesKey).longValue();
    }

    @Override
    public final long getPrefixesReceivedCount(final TablesKey tablesKey) {
        return this.prefixesReceived.get(tablesKey).longValue();
    }

    @Override
    public final long getPrefixesInstalledCount(final TablesKey tablesKey) {
        return this.prefixesInstalled.get(tablesKey).longValue();
    }

    @Override
    public final boolean isAfiSafiSupported(final TablesKey tablesKey) {
        return this.prefixesReceived.containsKey(tablesKey) && this.afiSafisAdvertized.contains(tablesKey);
    }

    @Override
    public final boolean isGracefulRestartAdvertized(final TablesKey tablesKey) {
        return this.afiSafisGracefulAdvertized.contains(tablesKey);
    }

    @Override
    public final boolean isGracefulRestartReceived(final TablesKey tablesKey) {
        return this.afiSafisGracefulReceived.contains(tablesKey);
    }

    @Override
    public final long getNegotiatedHoldTime() {
        return this.holdTimerValue;
    }

    @Override
    public final long getUpTime() {
        return this.sessionStopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Override
    public final PortNumber getLocalPort() {
        return this.localPort;
    }

    @Override
    public final IpAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    @Nonnull
    @Override
    public final PortNumber getRemotePort() {
        return this.remotePort;
    }

    @Override
    public final boolean isLocalRestarting() {
        return this.localRestarting;
    }

    @Override
    public final int isPeerRestartTime() {
        return this.peerRestartTime;
    }

    @Override
    public final boolean isPeerRestarting() {
        return this.peerRestarting;
    }

    @Override
    public final long getErroneousUpdateReceivedCount() {
        return this.erroneousUpdate.longValue();
    }

    @Override
    public final long getUpdateMessagesSentCount() {
        return this.updateSentCounter.longValue();
    }

    @Override
    public final long getNotificationMessagesSentCount() {
        return this.notificationSentCounter.longValue();
    }

    @Override
    public final long getUpdateMessagesReceivedCount() {
        return this.updateReceivedCounter.longValue();
    }

    @Override
    public final long getNotificationMessagesReceivedCount() {
        return this.notificationReceivedCounter.longValue();
    }

    @Override
    public final IpAddress getNeighborAddress() {
        return this.neighborAddress;
    }

    @Override
    public final synchronized long getTotalPrefixes() {
        return this.prefixesInstalled.values().stream().mapToLong(LongAdder::longValue).sum();
    }

    @Override
    public final boolean isAddPathCapabilitySupported() {
        return this.addPathCapability;
    }

    @Override
    public final boolean isAsn32CapabilitySupported() {
        return this.asn32Capability;
    }

    @Override
    public final boolean isGracefulRestartCapabilitySupported() {
        return this.gracefulRestartCapability;
    }

    @Override
    public final boolean isMultiProtocolCapabilitySupported() {
        return this.multiProtocolCapability;
    }

    @Override
    public final boolean isRouterRefreshCapabilitySupported() {
        return this.routerRefreshCapability;
    }

    @Override
    public final synchronized State getSessionState() {
        return this.sessionState;
    }

    @Override
    public final Set<TablesKey> getAfiSafisAdvertized() {
        return ImmutableSet.copyOf(this.afiSafisAdvertized);
    }

    @Override
    public final Set<TablesKey> getAfiSafisReceived() {
        return ImmutableSet.copyOf(this.prefixesReceived.keySet());
    }

    public final void setAfiSafiGracefulRestartState(final int peerRestartTime, final boolean peerRestarting,
        final boolean localRestarting) {
        this.peerRestartTime = peerRestartTime;
        this.peerRestarting = peerRestarting;
        this.localRestarting = localRestarting;
    }

    protected final synchronized void setAdvertizedGracefulRestartTableTypes(final List<TablesKey> receivedGraceful) {
        this.afiSafisGracefulReceived.addAll(receivedGraceful);
    }

    protected final void registerPrefixesSentCounter(final TablesKey tablesKey, final LongAdder prefixesSentCounter) {
        this.prefixesSent.put(tablesKey, prefixesSentCounter);
    }

    protected final void registerPrefixesCounters(@Nonnull final Map<TablesKey, LongAdder> prefixesReceived,
        @Nonnull final Map<TablesKey, LongAdder> prefixesInstalled) {
        this.prefixesReceived = prefixesReceived;
        this.prefixesInstalled = prefixesInstalled;
    }

    public final void resetState() {
        this.updateSentCounter.reset();
        this.notificationSentCounter.reset();
        this.updateReceivedCounter.reset();
        this.notificationReceivedCounter.reset();
        this.erroneousUpdate.reset();
        this.holdTimerValue = 0;
        this.remotePort = NON_DEFINED_PORT;
        this.localPort = NON_DEFINED_PORT;
        this.localRestarting = false;
        this.peerRestartTime = 0;
        this.peerRestarting = false;
        this.addPathCapability = false;
        this.asn32Capability = false;
        this.gracefulRestartCapability = false;
        this.multiProtocolCapability = false;
        this.routerRefreshCapability = false;
    }
}
