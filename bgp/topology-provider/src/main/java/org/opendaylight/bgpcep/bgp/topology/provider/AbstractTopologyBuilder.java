/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTopologyBuilder<T extends Route & DataObject>
        implements DataTreeChangeListener<T>, TopologyReference, FutureCallback<Empty> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyBuilder.class);
    // we limit the listener reset interval to be 5 min at most
    private static final long LISTENER_RESET_LIMIT_IN_MILLSEC = 5 * 60 * 1000;
    private static final int LISTENER_RESET_ENFORCE_COUNTER = 3;

    private final WithKey<Topology, TopologyKey> topology;
    private final RibReference locRibReference;
    private final DataBroker dataProvider;
    private final AddressFamily afi;
    private final SubsequentAddressFamily safi;
    private final TopologyKey topologyKey;
    private final TopologyTypes topologyTypes;
    private final long listenerResetLimitInMillsec;
    private final int listenerResetEnforceCounter;

    @GuardedBy("this")
    private Registration listenerRegistration = null;
    @GuardedBy("this")
    private TransactionChain chain = null;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    @GuardedBy("this")
    @VisibleForTesting
    protected long listenerScheduledRestartTime = 0;
    @GuardedBy("this")
    @VisibleForTesting
    protected int listenerScheduledRestartEnforceCounter = 0;
    protected boolean networkTopologyTransaction = true;

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes types, final AddressFamily afi,
            final SubsequentAddressFamily safi, final long listenerResetLimitInMillsec,
            final int listenerResetEnforceCounter) {
        this.dataProvider = dataProvider;
        this.locRibReference = requireNonNull(locRibReference);
        topologyKey = new TopologyKey(requireNonNull(topologyId));
        topologyTypes = types;
        this.afi = afi;
        this.safi = safi;
        this.listenerResetLimitInMillsec = listenerResetLimitInMillsec;
        this.listenerResetEnforceCounter = listenerResetEnforceCounter;
        topology = DataObjectIdentifier.builder(NetworkTopology.class).child(Topology.class, topologyKey).build();
    }

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
        final TopologyId topologyId, final TopologyTypes types, final AddressFamily afi,
        final SubsequentAddressFamily safi) {
        this(dataProvider, locRibReference, topologyId, types, afi, safi, LISTENER_RESET_LIMIT_IN_MILLSEC,
                LISTENER_RESET_ENFORCE_COUNTER);
    }

    public final synchronized void start() {
        LOG.debug("Initiating topology builder from {} at {}. AFI={}, SAFI={}", locRibReference, topology,
                afi, safi);
        initTransactionChain();
        initOperationalTopology();
        registerDataChangeListener();
    }

    /**
     * Register to data tree change listener.
     */
    private synchronized void registerDataChangeListener() {
        Preconditions.checkState(listenerRegistration == null,
            "Topology Listener on topology %s has been registered before.", getInstanceIdentifier());

        listenerRegistration = dataProvider.registerTreeChangeListener(
            LogicalDatastoreType.OPERATIONAL,
            getRouteWildcard(locRibReference.getInstanceIdentifier().toBuilder()
                .child(LocRib.class)
                .child(Tables.class, new TablesKey(afi, safi))
                .build()),
            this);
        LOG.debug("Registered listener {} on topology {}. Timestamp={}", this, getInstanceIdentifier(),
                listenerScheduledRestartTime);
    }

    /**
     * Unregister to data tree change listener.
     */
    private synchronized void unregisterDataChangeListener() {
        if (listenerRegistration != null) {
            LOG.debug("Unregistered listener {} on topology {}", this, getInstanceIdentifier());
            listenerRegistration.close();
            listenerRegistration = null;
        }
    }

    protected abstract DataObjectReference<T> getRouteWildcard(DataObjectIdentifier<Tables> tablesId);

    protected abstract void createObject(ReadWriteTransaction trans, DataObjectIdentifier<T> id, T value);

    protected abstract void removeObject(ReadWriteTransaction trans, DataObjectIdentifier<T> id, T value);

    protected abstract void clearTopology();

    @Override
    public final DataObjectIdentifier<Topology> getInstanceIdentifier() {
        return topology;
    }

    public final synchronized FluentFuture<? extends CommitInfo> close() {
        if (closed.getAndSet(true)) {
            LOG.trace("Transaction chain was already closed.");
            return CommitInfo.emptyFluentFuture();
        }
        LOG.info("Shutting down builder for {}", getInstanceIdentifier());
        unregisterDataChangeListener();
        final FluentFuture<? extends CommitInfo> future = destroyOperationalTopology();
        destroyTransactionChain();
        return future;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public synchronized void onDataTreeChanged(final List<DataTreeModification<T>> changes) {
        if (networkTopologyTransaction) {
            if (closed.get()) {
                LOG.trace("Transaction chain was already closed, skipping update.");
                return;
            }
            // check if the transaction chain needed to be restarted due to a previous error
            if (restartTransactionChainOnDemand()) {
                LOG.debug("The data change {} is disregarded due to restart of listener {}", changes, this);
                return;
            }
            final ReadWriteTransaction trans = chain.newReadWriteTransaction();
            LOG.trace("Received data change {} event with transaction {}", changes, trans.getIdentifier());
            final AtomicBoolean transactionInError = new AtomicBoolean(false);
            for (final DataTreeModification<T> change : changes) {
                try {
                    routeChanged(change, trans);
                } catch (final RuntimeException exc) {
                    LOG.warn("Data change {} (transaction {}) was not completely propagated to listener {}", change,
                            trans.getIdentifier(), this, exc);
                    // trans.cancel() is not supported by PingPongTransactionChain, so we just skip the problematic
                    // change.
                    // trans.commit() must be called first to unlock the current transaction chain, to make the chain
                    // closable so we cannot exit the #onDataTreeChanged() yet
                    transactionInError.set(true);
                    break;
                }
            }
            trans.commit().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    // as we are enforcing trans.commit(), in some cases the transaction execution actually could be
                    // successfully even when an exception is captured, thus #onTransactionChainFailed() never get
                    // invoked. Though the transaction chain remains usable,
                    // the data loss will not be able to be recovered. Thus we schedule a listener restart here
                    if (transactionInError.get()) {
                        LOG.warn("Transaction {} committed successfully while exception captured. Rescheduling a"
                                + " restart of listener {}", trans
                            .getIdentifier(), AbstractTopologyBuilder.this);
                        scheduleListenerRestart();
                    } else {
                        LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    // we do nothing but print out the log. Transaction chain restart will be done in
                    // #onTransactionChainFailed()
                    LOG.error("Failed to propagate change (transaction {}) by listener {}", trans.getIdentifier(),
                            AbstractTopologyBuilder.this, throwable);
                }
            }, MoreExecutors.directExecutor());
        } else {
            for (final DataTreeModification<T> change : changes) {
                routeChanged(change, null);
            }
        }
    }

    @VisibleForTesting
    protected void routeChanged(final DataTreeModification<T> change, final ReadWriteTransaction trans) {
        final var root = change.getRootNode();
        switch (root.modificationType()) {
            case DELETE:
                removeObject(trans, change.path(), root.dataBefore());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                final var before = root.dataBefore();
                if (before != null) {
                    removeObject(trans, change.path(), before);
                }
                createObject(trans, change.path(), root.dataAfter());
                break;
            default:
                throw new IllegalArgumentException("Unhandled modification type " + root.modificationType());
        }
    }

    private synchronized void initOperationalTopology() {
        requireNonNull(chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = chain.newWriteOnlyTransaction();
        trans.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, topology,
                new TopologyBuilder()
                    .withKey(topologyKey)
                    .setServerProvided(Boolean.TRUE)
                    .setTopologyTypes(topologyTypes)
                    .setLink(Map.of())
                    .setNode(Map.of())
                    .build());
        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to initialize topology {} (transaction {}) by listener {}",
                        AbstractTopologyBuilder.this.topology,
                        trans.getIdentifier(), AbstractTopologyBuilder.this, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Destroy the current operational topology data. Note a valid transaction must be provided.
     */
    private synchronized FluentFuture<? extends CommitInfo> destroyOperationalTopology() {
        requireNonNull(chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        final FluentFuture<? extends CommitInfo> future = trans.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Operational topology removed {}", AbstractTopologyBuilder.this.topology);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Unable to reset operational topology {} (transaction {})",
                    AbstractTopologyBuilder.this.topology, trans.getIdentifier(), throwable);
            }
        }, MoreExecutors.directExecutor());
        clearTopology();
        return future;
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one.
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for topology {}", this);
        Preconditions.checkState(chain == null,
                "Transaction chain has to be closed before being initialized");
        chain = dataProvider.createMergingTransactionChain();
        chain.addCallback(this);
    }

    /**
     * Destroy the current transaction chain.
     */
    private synchronized void destroyTransactionChain() {
        if (chain != null) {
            LOG.debug("Destroy transaction chain for topology {}", this);
            // we cannot close the transaction chain, as it will close the AbstractDOMForwardedTransactionFactory
            // and the transaction factory cannot be reopen even if we recreate the transaction chain
            // so we abandon the chain directly
            // FIXME we want to close the transaction chain gracefully once the PingPongTransactionChain get improved
            // and the above problem get resolved.
//            try {
//                this.chain.close();
//            } catch (Exception e) {
//                // the close() may not succeed when the transaction chain is locked
//                LOG.error("Unable to close transaction chain {} for topology builder {}", this.chain,
//                  getInstanceIdentifier());
//            }
            chain = null;
        }
    }

    /**
     * Reset the data change listener to its initial status.
     * By resetting the listener we will be able to recover all the data lost before
     */
    @VisibleForTesting
    protected synchronized void resetListener() {
        requireNonNull(listenerRegistration, "Listener on topology " + this + " hasn't been initialized.");
        LOG.debug("Resetting data change listener for topology builder {}", getInstanceIdentifier());
        // unregister current listener to prevent incoming data tree change first
        unregisterDataChangeListener();
        // create new transaction chain to reset the chain status
        resetTransactionChain();
        // reset the operational topology data so that we can have clean status
        destroyOperationalTopology();
        initOperationalTopology();
        // re-register the data change listener to reset the operational topology
        // we are expecting to receive all the pre-exist route change on the next onDataTreeChanged() call
        registerDataChangeListener();
    }

    /**
     * Reset the transaction chain only so that the PingPong transaction chain will become usable again.
     * However, there will be data loss if we do not apply the previous failed transaction again
     */
    @VisibleForTesting
    protected synchronized void resetTransactionChain() {
        LOG.debug("Resetting transaction chain for topology builder {}", getInstanceIdentifier());
        destroyTransactionChain();
        initTransactionChain();
    }

    /**
     * There are a few reasons we want to schedule a listener restart in a delayed manner:
     * 1. we should avoid restarting the listener as when the topology is big, there might be huge overhead
     * rebuilding the whole linkstate topology again and again
     * 2. the #onTransactionChainFailed() normally get invoked after a delay. During that time gap, more
     * data changes might still be pushed to #onDataTreeChanged(). And because #onTransactionChainFailed()
     * is not invoked yet, listener restart/transaction chain restart is not done. Thus the new changes
     * will still cause error and another #onTransactionChainFailed() might be invoked later. The listener
     * will be restarted again in that case, which is unexpected. Restarting of transaction chain only introduce
     * little overhead and it's okay to be restarted within a small time window.
     * Note: when the listener is restarted, we can disregard all the incoming data changes before the restart is
     * done, as after the listener unregister/reregister, the first #onDataTreeChanged() call will contain the a
     * complete set of existing changes
     *
     * @return if the listener get restarted, return true; otherwise false
     */
    @VisibleForTesting
    protected synchronized boolean restartTransactionChainOnDemand() {
        if (listenerScheduledRestartTime > 0) {
            // when the #this.listenerScheduledRestartTime timer timed out we can reset the listener,
            // otherwise we should only reset the transaction chain
            if (System.currentTimeMillis() > listenerScheduledRestartTime) {
                // reset the the restart timer
                listenerScheduledRestartTime = 0;
                listenerScheduledRestartEnforceCounter = 0;
                resetListener();
                return true;
            }

            resetTransactionChain();
        }
        return false;
    }

    @VisibleForTesting
    protected synchronized void scheduleListenerRestart() {
        if (0 == listenerScheduledRestartTime) {
            listenerScheduledRestartTime = System.currentTimeMillis() + listenerResetLimitInMillsec;
        } else if (System.currentTimeMillis() > listenerScheduledRestartTime
            && ++listenerScheduledRestartEnforceCounter < listenerResetEnforceCounter) {
            // if the transaction failure happens again, we will delay the listener restart up to
            // #LISTENER_RESET_LIMIT_IN_MILLSEC times
            listenerScheduledRestartTime += listenerResetLimitInMillsec;
        }
        LOG.debug("A listener restart was scheduled at {} (current system time is {})",
                listenerScheduledRestartTime, System.currentTimeMillis());
    }

    @Override
    public final synchronized void onFailure(final Throwable cause) {
        LOG.error("Topology builder for {} failed", getInstanceIdentifier(), cause);
        scheduleListenerRestart();
        restartTransactionChainOnDemand();
    }

    @Override
    public final void onSuccess(final Empty value) {
        LOG.info("Topology builder for {} shut down", getInstanceIdentifier());
    }
}
