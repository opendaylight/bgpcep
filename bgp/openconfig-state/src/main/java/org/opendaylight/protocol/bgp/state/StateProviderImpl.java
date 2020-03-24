/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is thread-safe
public final class StateProviderImpl implements TransactionChainListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(StateProviderImpl.class);
    private final BGPStateConsumer stateCollector;
    private final BGPTableTypeRegistryConsumer bgpTableTypeRegistry;
    private final KeyedInstanceIdentifier<NetworkInstance, NetworkInstanceKey> networkInstanceIId;
    private final int timeout;
    private final DataBroker dataBroker;
    @GuardedBy("this")
    private final Map<String, InstanceIdentifier<Bgp>> instanceIdentifiersCache = new HashMap<>();
    @GuardedBy("this")
    private TransactionChain transactionChain;
    @GuardedBy("this")
    private ScheduledFuture<?> scheduleTask;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public StateProviderImpl(final @NonNull DataBroker dataBroker, final int timeout,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry,
            final @NonNull BGPStateConsumer stateCollector, final @NonNull String networkInstanceName) {
        this(dataBroker, timeout, bgpTableTypeRegistry, stateCollector, networkInstanceName,
                Executors.newScheduledThreadPool(1));
    }

    public StateProviderImpl(final @NonNull DataBroker dataBroker, final int timeout,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry,
            final @NonNull BGPStateConsumer stateCollector,
            final @NonNull String networkInstanceName, final @NonNull ScheduledExecutorService scheduler) {
        this.dataBroker = requireNonNull(dataBroker);
        this.bgpTableTypeRegistry = requireNonNull(bgpTableTypeRegistry);
        this.stateCollector = requireNonNull(stateCollector);
        this.networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName));
        this.timeout = timeout;
        this.scheduler = scheduler;
    }

    public synchronized void init() {
        this.transactionChain = this.dataBroker.createMergingTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            @SuppressWarnings("checkstyle:IllegalCatch")
            public void run() {
                synchronized (StateProviderImpl.this) {
                    final WriteTransaction wTx = StateProviderImpl.this.transactionChain.newWriteOnlyTransaction();
                    try {
                        updateBGPStats(wTx);

                        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
                            @Override
                            public void onSuccess(final CommitInfo result) {
                                LOG.debug("Successfully committed BGP stats update");
                            }

                            @Override
                            public void onFailure(final Throwable ex) {
                                LOG.error("Failed to commit BGP stats update", ex);
                            }
                        }, MoreExecutors.directExecutor());
                    } catch (final Exception e) {
                        LOG.warn("Failed to prepare Tx for BGP stats update", e);
                        wTx.cancel();
                    }
                }
            }
        };

        this.scheduleTask = this.scheduler.scheduleAtFixedRate(task, 0, this.timeout, SECONDS);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private synchronized void updateBGPStats(final WriteTransaction wtx) {
        final Set<String> oldStats = new HashSet<>(this.instanceIdentifiersCache.keySet());
        this.stateCollector.getRibStats().stream().filter(BGPRibState::isActive).forEach(bgpStateConsumer -> {
            final KeyedInstanceIdentifier<Rib, RibKey> ribId = bgpStateConsumer.getInstanceIdentifier();
            final List<BGPPeerState> peerStats = this.stateCollector.getPeerStats().stream()
                    .filter(BGPPeerState::isActive).filter(peerState -> ribId.equals(peerState.getInstanceIdentifier()))
                    .collect(Collectors.toList());
            storeOperationalState(bgpStateConsumer, peerStats, ribId.getKey().getId().getValue(), wtx);
            oldStats.remove(ribId.getKey().getId().getValue());
        });
        oldStats.forEach(ribId -> removeStoredOperationalState(ribId, wtx));
    }

    private synchronized void removeStoredOperationalState(final String ribId, final WriteTransaction wtx) {
        final InstanceIdentifier<Bgp> bgpIID = this.instanceIdentifiersCache.remove(ribId);
        wtx.delete(LogicalDatastoreType.OPERATIONAL, bgpIID);
    }

    private synchronized void storeOperationalState(final BGPRibState bgpStateConsumer,
            final List<BGPPeerState> peerStats, final String ribId, final WriteTransaction wtx) {
        final Global global = GlobalUtil.buildGlobal(bgpStateConsumer, this.bgpTableTypeRegistry);
        final PeerGroups peerGroups = PeerGroupUtil.buildPeerGroups(peerStats);
        final Neighbors neighbors = NeighborUtil.buildNeighbors(peerStats, this.bgpTableTypeRegistry);
        InstanceIdentifier<Bgp> bgpIID = this.instanceIdentifiersCache.get(ribId);
        if (bgpIID == null) {
            final ProtocolKey protocolKey = new ProtocolKey(BGP.class, bgpStateConsumer.getInstanceIdentifier()
                    .getKey().getId().getValue());
            final KeyedInstanceIdentifier<Protocol, ProtocolKey> protocolIId = this.networkInstanceIId
                    .child(Protocols.class).child(Protocol.class, protocolKey);
            bgpIID = protocolIId.augmentation(NetworkInstanceProtocol.class).child(Bgp.class);
            this.instanceIdentifiersCache.put(ribId, bgpIID);
        }

        final Bgp bgp = new BgpBuilder().setGlobal(global).setNeighbors(neighbors).setPeerGroups(peerGroups).build();
        wtx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, bgpIID, bgp);
    }

    @Override
    public synchronized void close() {
        if (closed.compareAndSet(false, true)) {
            this.scheduleTask.cancel(true);
            if (!this.instanceIdentifiersCache.keySet().isEmpty()) {
                final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
                this.instanceIdentifiersCache.values()
                        .forEach(bgpIID -> wtx.delete(LogicalDatastoreType.OPERATIONAL, bgpIID));
                this.instanceIdentifiersCache.clear();
                wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
                    @Override
                    public void onSuccess(final CommitInfo result) {
                        LOG.trace("Successfully operational stats removed.");
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOG.error("Failed to clean up operational stats", throwable);
                    }
                }, MoreExecutors.directExecutor());
            }
            this.transactionChain.close();
            this.scheduler.shutdown();
        }
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain chain, final Transaction transaction,
            final Throwable cause) {
        LOG.error("Transaction chain {} failed for tx {}",
                chain, transaction != null ? transaction.getIdentifier() : null, cause);

        if (!closed.get()) {
            transactionChain.close();
            transactionChain = dataBroker.createMergingTransactionChain(this);
        }
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }
}
