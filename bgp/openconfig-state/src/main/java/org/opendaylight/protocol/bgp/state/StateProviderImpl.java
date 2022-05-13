/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteOperations;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroups;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.OpenconfigNetworkInstanceData;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is thread-safe
@Singleton
@Component(service = {})
@Designate(ocd = StateProviderImpl.Configuration.class)
@RequireServiceComponentRuntime
public final class StateProviderImpl implements TransactionChainListener, AutoCloseable {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition(description = "Name of the OpenConfig network instance to which to bind")
        String networkInstanceName() default "global-bgp";

        @AttributeDefinition(description = "Statistics update interval, in seconds", min = "1")
        int updateIntervalSeconds() default 5;
    }

    private static final Logger LOG = LoggerFactory.getLogger(StateProviderImpl.class);

    private final BGPStateProvider stateProvider;
    private final BGPTableTypeRegistryConsumer bgpTableTypeRegistry;
    private final KeyedInstanceIdentifier<NetworkInstance, NetworkInstanceKey> networkInstanceIId;
    private final DataBroker dataBroker;
    @GuardedBy("this")
    private final Map<String, InstanceIdentifier<Bgp>> instanceIdentifiersCache = new HashMap<>();
    @GuardedBy("this")
    private TransactionChain transactionChain;
    @GuardedBy("this")
    private final ScheduledFuture<?> scheduleTask;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Activate
    public StateProviderImpl(@Reference final @NonNull DataBroker dataBroker,
            @Reference final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry,
            @Reference final @NonNull BGPStateProvider stateProvider, final @NonNull Configuration configuration) {
        this(dataBroker, configuration.updateIntervalSeconds(), bgpTableTypeRegistry, stateProvider,
                configuration.networkInstanceName());
    }

    @Inject
    public StateProviderImpl(final @NonNull DataBroker dataBroker, final int timeout,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry,
            final @NonNull BGPStateProvider stateProvider,
            final @NonNull String networkInstanceName) {
        this(dataBroker, timeout, TimeUnit.SECONDS, bgpTableTypeRegistry, stateProvider, networkInstanceName,
                Executors.newScheduledThreadPool(1));
    }

    @VisibleForTesting
    StateProviderImpl(final @NonNull DataBroker dataBroker, final long period, final TimeUnit timeUnit,
            final @NonNull BGPTableTypeRegistryConsumer bgpTableTypeRegistry,
            final @NonNull BGPStateProvider stateProvider,
            final @NonNull String networkInstanceName, final @NonNull ScheduledExecutorService scheduler) {
        this.dataBroker = requireNonNull(dataBroker);
        this.bgpTableTypeRegistry = requireNonNull(bgpTableTypeRegistry);
        this.stateProvider = requireNonNull(stateProvider);
        networkInstanceIId =
            InstanceIdentifier.builderOfInherited(OpenconfigNetworkInstanceData.class, NetworkInstances.class).build()
                .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName));
        this.scheduler = scheduler;

        transactionChain = this.dataBroker.createMergingTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            @SuppressWarnings("checkstyle:IllegalCatch")
            public void run() {
                synchronized (StateProviderImpl.this) {
                    final WriteTransaction wTx = transactionChain.newWriteOnlyTransaction();
                    try {
                        updateBGPStats(wTx);
                    } catch (final Exception e) {
                        LOG.warn("Failed to prepare Tx for BGP stats update", e);
                        wTx.cancel();
                        return;
                    }

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
                }
            }
        };

        scheduleTask = this.scheduler.scheduleAtFixedRate(task, 0, period, timeUnit);
    }

    private synchronized void updateBGPStats(final WriteOperations wtx) {
        final Set<String> oldStats = new HashSet<>(instanceIdentifiersCache.keySet());
        stateProvider.getRibStats().stream().filter(BGPRibState::isActive).forEach(bgpStateConsumer -> {
            final KeyedInstanceIdentifier<Rib, RibKey> ribId = bgpStateConsumer.getInstanceIdentifier();
            final List<BGPPeerState> peerStats = stateProvider.getPeerStats().stream()
                    .filter(BGPPeerState::isActive).filter(peerState -> ribId.equals(peerState.getInstanceIdentifier()))
                    .collect(Collectors.toList());
            storeOperationalState(bgpStateConsumer, peerStats, ribId.getKey().getId().getValue(), wtx);
            oldStats.remove(ribId.getKey().getId().getValue());
        });
        oldStats.forEach(ribId -> removeStoredOperationalState(ribId, wtx));
    }

    private synchronized void removeStoredOperationalState(final String ribId, final WriteOperations wtx) {
        final InstanceIdentifier<Bgp> bgpIID = instanceIdentifiersCache.remove(ribId);
        wtx.delete(LogicalDatastoreType.OPERATIONAL, bgpIID);
    }

    private synchronized void storeOperationalState(final BGPRibState bgpStateConsumer,
            final List<BGPPeerState> peerStats, final String ribId, final WriteOperations wtx) {
        final Global global = GlobalUtil.buildGlobal(bgpStateConsumer, bgpTableTypeRegistry);
        final PeerGroups peerGroups = PeerGroupUtil.buildPeerGroups(peerStats);
        final Neighbors neighbors = NeighborUtil.buildNeighbors(peerStats, bgpTableTypeRegistry);
        InstanceIdentifier<Bgp> bgpIID = instanceIdentifiersCache.get(ribId);
        if (bgpIID == null) {
            final ProtocolKey protocolKey = new ProtocolKey(BGP.class, bgpStateConsumer.getInstanceIdentifier()
                    .getKey().getId().getValue());
            final KeyedInstanceIdentifier<Protocol, ProtocolKey> protocolIId = networkInstanceIId
                    .child(Protocols.class).child(Protocol.class, protocolKey);
            bgpIID = protocolIId.augmentation(NetworkInstanceProtocol.class).child(Bgp.class);
            instanceIdentifiersCache.put(ribId, bgpIID);
        }

        final Bgp bgp = new BgpBuilder().setGlobal(global).setNeighbors(neighbors).setPeerGroups(peerGroups).build();
        wtx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, bgpIID, bgp);
    }

    @Deactivate
    @PreDestroy
    @Override
    public synchronized void close() {
        if (closed.compareAndSet(false, true)) {
            scheduleTask.cancel(true);
            if (!instanceIdentifiersCache.isEmpty()) {
                final WriteTransaction wTx = transactionChain.newWriteOnlyTransaction();
                instanceIdentifiersCache.values()
                        .forEach(bgpIID -> wTx.delete(LogicalDatastoreType.OPERATIONAL, bgpIID));
                instanceIdentifiersCache.clear();
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
            transactionChain.close();
            scheduler.shutdown();
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
