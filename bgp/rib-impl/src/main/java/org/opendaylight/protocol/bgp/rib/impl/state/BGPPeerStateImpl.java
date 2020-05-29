/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPMessagesListener;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesSentCounters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPLlGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState.Mode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class BGPPeerStateImpl extends DefaultRibReference implements BGPPeerState, BGPAfiSafiState,
        BGPGracelfulRestartState, BGPLlGracelfulRestartState,BGPErrorHandlingState, BGPPeerMessagesState,
        BGPPeerStateConsumer, BGPMessagesListener {
    private static final long NONE = 0L;
    private final IpAddressNoZone neighborAddress;
    private final Set<TablesKey> afiSafisAdvertized;
    private final Set<TablesKey> afiSafisGracefulAdvertized;
    private final Set<TablesKey> afiSafisGracefulReceived = new HashSet<>();
    private final Map<TablesKey, Integer> afiSafisLlGracefulAdvertised;
    private final Map<TablesKey, Integer> afiSafisLlGracefulReceived = new HashMap<>();
    private final LongAdder updateSentCounter = new LongAdder();
    private final LongAdder notificationSentCounter = new LongAdder();
    private final LongAdder updateReceivedCounter = new LongAdder();
    private final LongAdder notificationReceivedCounter = new LongAdder();
    private final LongAdder erroneousUpdate = new LongAdder();
    private final String groupId;
    private AtomicBoolean active = new AtomicBoolean(false);

    @GuardedBy("this")
    private final Map<TablesKey, PrefixesSentCounters> prefixesSent = new HashMap<>();
    @GuardedBy("this")
    private PrefixesReceivedCounters prefixesReceived;
    @GuardedBy("this")
    private PrefixesInstalledCounters prefixesInstalled;
    @GuardedBy("this")
    private boolean localRestarting;
    @GuardedBy("this")
    private int peerRestartTime;
    @GuardedBy("this")
    private boolean peerRestarting;

    public BGPPeerStateImpl(final @NonNull KeyedInstanceIdentifier<Rib, RibKey> instanceIdentifier,
            final @Nullable String groupId, final @NonNull IpAddressNoZone neighborAddress,
            final @NonNull Set<TablesKey> afiSafisAdvertized,
            final @NonNull Set<TablesKey> afiSafisGracefulAdvertized,
            final @NonNull Map<TablesKey, Integer> afiSafisLlGracefulAdvertized) {
        super(instanceIdentifier);
        this.neighborAddress = requireNonNull(neighborAddress);
        this.groupId = groupId;
        this.afiSafisAdvertized = requireNonNull(afiSafisAdvertized);
        this.afiSafisGracefulAdvertized = requireNonNull(afiSafisGracefulAdvertized);
        this.afiSafisLlGracefulAdvertised = requireNonNull(afiSafisLlGracefulAdvertized);
    }

    @Override
    public final String getGroupId() {
        return this.groupId;
    }

    @Override
    public final IpAddressNoZone getNeighborAddress() {
        return this.neighborAddress;
    }

    @Override
    public final synchronized long getTotalPrefixes() {
        if (this.prefixesInstalled == null) {
            return NONE;
        }
        return this.prefixesInstalled.getTotalPrefixesInstalled();
    }

    @Override
    public final BGPPeerMessagesState getBGPPeerMessagesState() {
        return this;
    }

    @Override
    public final BGPGracelfulRestartState getBGPGracelfulRestart() {
        return this;
    }

    @Override
    public final synchronized boolean isAfiSafiSupported(final TablesKey tablesKey) {
        return this.prefixesReceived != null && this.prefixesReceived.isSupported(tablesKey)
                && this.afiSafisAdvertized.contains(tablesKey);
    }

    @Override
    public final synchronized long getPrefixesInstalledCount(final TablesKey tablesKey) {
        if (this.prefixesInstalled == null) {
            return NONE;
        }
        return this.prefixesInstalled.getPrefixedInstalledCount(tablesKey);
    }

    @Override
    public final synchronized long getPrefixesSentCount(final TablesKey tablesKey) {
        if (this.prefixesSent == null) {
            return 0;
        }
        final PrefixesSentCounters counter = this.prefixesSent.get(tablesKey);
        if (counter == null) {
            return NONE;
        }
        return counter.getPrefixesSentCount();
    }

    @Override
    public final synchronized long getPrefixesReceivedCount(final TablesKey tablesKey) {
        if (this.prefixesReceived == null) {
            return NONE;
        }
        return this.prefixesReceived.getPrefixedReceivedCount(tablesKey);
    }

    @Override
    public final Set<TablesKey> getAfiSafisAdvertized() {
        return ImmutableSet.copyOf(this.afiSafisAdvertized);
    }

    @Override
    public final synchronized Set<TablesKey> getAfiSafisReceived() {
        if (this.prefixesReceived == null) {
            return Collections.emptySet();
        }
        return this.prefixesReceived.getTableKeys();
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
    public final synchronized boolean isLocalRestarting() {
        return this.localRestarting;
    }

    @Override
    public final synchronized int getPeerRestartTime() {
        return this.peerRestartTime;
    }

    @Override
    public final synchronized boolean isPeerRestarting() {
        return this.peerRestarting;
    }

    public final synchronized void setAfiSafiGracefulRestartState(final int newPeerRestartTime,
            final boolean newPeerRestarting, final boolean newLocalRestarting) {
        this.peerRestartTime = newPeerRestartTime;
        this.peerRestarting = newPeerRestarting;
        this.localRestarting = newLocalRestarting;
    }

    protected final synchronized void setAdvertizedGracefulRestartTableTypes(final List<TablesKey> receivedGraceful) {
        this.afiSafisGracefulReceived.clear();
        this.afiSafisGracefulReceived.addAll(receivedGraceful);
    }

    protected final synchronized void registerPrefixesSentCounter(final TablesKey tablesKey,
        final PrefixesSentCounters prefixesSentCounter) {
        this.prefixesSent.put(tablesKey, prefixesSentCounter);
    }

    protected final synchronized void registerPrefixesCounters(
            final @NonNull PrefixesReceivedCounters newPrefixesReceived,
            final @NonNull PrefixesInstalledCounters newPrefixesInstalled) {
        this.prefixesReceived = newPrefixesReceived;
        this.prefixesInstalled = newPrefixesInstalled;
    }

    protected final synchronized void resetState() {
        this.localRestarting = false;
        this.peerRestartTime = 0;
        this.peerRestarting = false;
    }

    protected final synchronized void setRestartingState() {
        this.peerRestarting = true;
    }

    protected final synchronized void setLocalRestartingState(final boolean restarting) {
        this.localRestarting = restarting;
    }

    @Override
    public final BGPPeerState getPeerState() {
        return this;
    }

    @Override
    public final long getErroneousUpdateReceivedCount() {
        //FIXME BUG-4979
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
    public final void messageSent(final Notification msg) {
        if (msg instanceof Notify) {
            this.notificationSentCounter.increment();
        } else if (msg instanceof Update) {
            this.updateSentCounter.increment();
        }
    }

    @Override
    public final void messageReceived(final Notification msg) {
        if (msg instanceof Notify) {
            this.notificationReceivedCounter.increment();
        } else if (msg instanceof Update) {
            this.updateReceivedCounter.increment();
        }
    }

    @Override
    public final boolean isActive() {
        return this.active.get();
    }

    protected final void setActive(final boolean active) {
        this.active.set(active);
    }

    @Override
    public final synchronized Mode getMode() {
        if (this.afiSafisGracefulAdvertized.isEmpty()) {
            return Mode.HELPERONLY;
        }
        if (this.afiSafisGracefulReceived.isEmpty()) {
            return Mode.REMOTEHELPER;
        }
        return Mode.BILATERAL;
    }

    public final synchronized void setAdvertizedLlGracefulRestartTableTypes(
            final Map<TablesKey, Integer> afiSafiReceived) {
        this.afiSafisLlGracefulReceived.clear();
        this.afiSafisLlGracefulReceived.putAll(afiSafiReceived);
    }

    @Override
    public final synchronized boolean isLlGracefulRestartAdvertised(final TablesKey tablesKey) {
        return this.afiSafisLlGracefulAdvertised.containsKey(tablesKey);
    }

    @Override
    public final synchronized boolean isLlGracefulRestartReceived(final TablesKey tablesKey) {
        return this.afiSafisLlGracefulReceived.containsKey(tablesKey);
    }

    @Override
    public final synchronized int getLlGracefulRestartTimer(final TablesKey tablesKey) {
        final int timerAdvertised = isLlGracefulRestartAdvertised(tablesKey)
                ? this.afiSafisLlGracefulAdvertised.get(tablesKey) : 0;
        final int timerReceived = isLlGracefulRestartReceived(tablesKey)
                ? this.afiSafisLlGracefulReceived.get(tablesKey) : 0;
        return Integer.min(timerAdvertised, timerReceived);
    }
}
