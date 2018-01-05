/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state;

import static java.util.Objects.requireNonNull;

import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.RibKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
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
    private BindingTransactionChain transactionChain;
    @GuardedBy("this")
    private ScheduledFuture<?> scheduleTask;

    public StateProviderImpl(@Nonnull final DataBroker dataBroker, final int timeout,
            @Nonnull final BGPTableTypeRegistryConsumer bgpTableTypeRegistry,
            @Nonnull final BGPStateConsumer stateCollector,
            @Nonnull final String networkInstanceName) {
        this.dataBroker = requireNonNull(dataBroker);
        this.bgpTableTypeRegistry = requireNonNull(bgpTableTypeRegistry);
        this.stateCollector = requireNonNull(stateCollector);
        this.networkInstanceIId = InstanceIdentifier.create(NetworkInstances.class)
                .child(NetworkInstance.class, new NetworkInstanceKey(networkInstanceName));
        this.timeout = timeout;
    }

    public synchronized void init() {
        this.transactionChain = this.dataBroker.createTransactionChain(this);
        final TimerTask task = new TimerTask() {
            @Override
            @SuppressWarnings("checkstyle:IllegalCatch")
            public void run() {
                synchronized (StateProviderImpl.this) {
                    final WriteTransaction wTx = StateProviderImpl.this.transactionChain.newWriteOnlyTransaction();
                    try {
                        updateBGPStats(wTx);
                    } catch (final Exception e) {
                        LOG.warn("Failed to update BGP Stats", e);
                    } finally {
                        wTx.submit();
                    }
                }
            }
        };

        this.scheduleTask = GlobalEventExecutor.INSTANCE.scheduleAtFixedRate(task, 0, this.timeout,
                TimeUnit.SECONDS);
    }

    private synchronized void updateBGPStats(final WriteTransaction wtx) {
        final Set<String> oldStats = new HashSet<>(this.instanceIdentifiersCache.keySet());
        this.stateCollector.getRibStats().stream().filter(BGPRIBState::isActive).forEach(bgpStateConsumer -> {
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

    private synchronized void storeOperationalState(final BGPRIBState bgpStateConsumer,
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
            bgpIID = protocolIId.augmentation(Protocol1.class).child(Bgp.class);
            this.instanceIdentifiersCache.put(ribId, bgpIID);
        }

        final Bgp bgp = new BgpBuilder().setGlobal(global).setNeighbors(neighbors).setPeerGroups(peerGroups).build();
        wtx.put(LogicalDatastoreType.OPERATIONAL, bgpIID, bgp, WriteTransaction.CREATE_MISSING_PARENTS);
    }

    @Override
    public synchronized void close() throws Exception {
        this.scheduleTask.cancel(true);
        if (!this.instanceIdentifiersCache.keySet().isEmpty()) {
            final WriteTransaction wTx = this.transactionChain.newWriteOnlyTransaction();
            this.instanceIdentifiersCache.keySet().iterator()
                    .forEachRemaining(ribId -> removeStoredOperationalState(ribId, wTx));
            wTx.submit().get();
        }
        this.transactionChain.close();
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        LOG.error("Transaction chain failed {}.", transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("Transaction chain {} successful.", chain);
    }
}
