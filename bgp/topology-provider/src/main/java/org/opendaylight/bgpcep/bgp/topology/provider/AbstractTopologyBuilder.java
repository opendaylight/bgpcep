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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTopologyBuilder<T extends Route> implements ClusteredDataTreeChangeListener<T>,
    TopologyReference, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyBuilder.class);
    // we limit the listener reset interval to be 5 min at most
    private static final long LISTENER_RESET_LIMIT_IN_MILLSEC = 5 * 60 * 1000;
    private static final int LISTENER_RESET_ENFORCE_COUNTER = 3;
    private final InstanceIdentifier<Topology> topology;
    private final RibReference locRibReference;
    private final DataBroker dataProvider;
    private final Class<? extends AddressFamily> afi;
    private final Class<? extends SubsequentAddressFamily> safi;
    private final TopologyKey topologyKey;
    private final TopologyTypes topologyTypes;
    private final long listenerResetLimitInMillsec;
    private final int listenerResetEnforceCounter;

    @GuardedBy("this")
    private ListenerRegistration<AbstractTopologyBuilder<T>> listenerRegistration = null;
    @GuardedBy("this")
    private BindingTransactionChain chain = null;
    @GuardedBy("this")
    private boolean closed = false;
    @GuardedBy("this")
    @VisibleForTesting
    protected long listenerScheduledRestartTime = 0;
    @GuardedBy("this")
    @VisibleForTesting
    protected int listenerScheduledRestartEnforceCounter = 0;

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes types, final Class<? extends AddressFamily> afi,
        final Class<? extends SubsequentAddressFamily> safi, final long listenerResetLimitInMillsec,
        final int listenerResetEnforceCounter) {
        this.dataProvider = dataProvider;
        this.locRibReference = requireNonNull(locRibReference);
        this.topologyKey = new TopologyKey(requireNonNull(topologyId));
        this.topologyTypes = types;
        this.afi = afi;
        this.safi = safi;
        this.listenerResetLimitInMillsec = listenerResetLimitInMillsec;
        this.listenerResetEnforceCounter = listenerResetEnforceCounter;
        this.topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, this.topologyKey).build();
    }

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
        final TopologyId topologyId, final TopologyTypes types, final Class<? extends AddressFamily> afi,
        final Class<? extends SubsequentAddressFamily> safi) {
        this(dataProvider, locRibReference, topologyId, types, afi, safi, LISTENER_RESET_LIMIT_IN_MILLSEC, LISTENER_RESET_ENFORCE_COUNTER);
    }

    public final synchronized void start() {
        LOG.debug("Initiating topology builder from {} at {}. AFI={}, SAFI={}", this.locRibReference, this.topology, this.afi, this.safi);
        initTransactionChain();
        initOperationalTopology();
        registerDataChangeListener();
    }

    /**
     * Register to data tree change listener
     */
    private synchronized void registerDataChangeListener() {
        Preconditions.checkState(this.listenerRegistration == null, "Topology Listener on topology %s has been registered before.", this.getInstanceIdentifier());
        final InstanceIdentifier<Tables> tablesId = this.locRibReference.getInstanceIdentifier().child(LocRib.class).child(Tables.class, new TablesKey(this.afi, this.safi));
        final DataTreeIdentifier<T> id = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getRouteWildcard(tablesId));

        this.listenerRegistration = this.dataProvider.registerDataTreeChangeListener(id, this);
        LOG.debug("Registered listener {} on topology {}. Timestamp={}", this, this.getInstanceIdentifier(), this.listenerScheduledRestartTime);
    }

    /**
     * Unregister to data tree change listener
     */
    private final synchronized void unregisterDataChangeListener() {
        if (this.listenerRegistration != null) {
            LOG.debug("Unregistered listener {} on topology {}", this, this.getInstanceIdentifier());
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
    }

    protected abstract InstanceIdentifier<T> getRouteWildcard(InstanceIdentifier<Tables> tablesId);

    protected abstract void createObject(ReadWriteTransaction trans, InstanceIdentifier<T> id, T value);

    protected abstract void removeObject(ReadWriteTransaction trans, InstanceIdentifier<T> id, T value);

    protected abstract void clearTopology();

    @Override
    public final InstanceIdentifier<Topology> getInstanceIdentifier() {
        return this.topology;
    }

    public final synchronized ListenableFuture<Void> close() {
        if (this.closed) {
            LOG.trace("Transaction chain was already closed.");
            Futures.immediateFuture(null);
        }
        this.closed = true;
        LOG.info("Shutting down builder for {}", getInstanceIdentifier());
        unregisterDataChangeListener();
        final ListenableFuture<Void> future = destroyOperationalTopology();
        destroyTransactionChain();
        return future;
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<T>> changes) {
        if (this.closed) {
            LOG.trace("Transaction chain was already closed, skipping update.");
            return;
        }
        // check if the transaction chain needed to be restarted due to a previous error
        if (restartTransactionChainOnDemand()) {
            LOG.debug("The data change {} is disregarded due to restart of listener {}", changes, this);
            return;
        }
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        LOG.trace("Received data change {} event with transaction {}", changes, trans.getIdentifier());
        final AtomicBoolean transactionInError = new AtomicBoolean(false);
        for (final DataTreeModification<T> change : changes) {
            try {
                routeChanged(change, trans);
            } catch (final RuntimeException e) {
                LOG.warn("Data change {} (transaction {}) was not completely propagated to listener {}", change, trans.getIdentifier(), this, e);
                // trans.cancel() is not supported by PingPongTransactionChain, so we just skip the problematic change
                // trans.submit() must be called first to unlock the current transaction chain, to make the chain closable
                // so we cannot exit the #onDataTreeChanged() yet
                transactionInError.set(true);
                break;
            }
        }
        Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                // as we are enforcing trans.submit(), in some cases the transaction execution actually could be successfully even when an
                // exception is captured, thus #onTransactionChainFailed() never get invoked. Though the transaction chain remains usable,
                // the data loss will not be able to be recovered. Thus we schedule a listener restart here
                if (transactionInError.get()) {
                    LOG.warn("Transaction {} committed successfully while exception captured. Rescheduling a restart of listener {}", trans
                        .getIdentifier(), AbstractTopologyBuilder.this);
                    scheduleListenerRestart();
                } else {
                    LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                // we do nothing but print out the log. Transaction chain restart will be done in #onTransactionChainFailed()
                LOG.error("Failed to propagate change (transaction {}) by listener {}", trans.getIdentifier(), AbstractTopologyBuilder.this, t);
            }
        }, MoreExecutors.directExecutor());
    }

    @VisibleForTesting
    protected void routeChanged(final DataTreeModification<T> change, final ReadWriteTransaction trans) {
        final DataObjectModification<T> root = change.getRootNode();
        switch (root.getModificationType()) {
        case DELETE:
            removeObject(trans, change.getRootPath().getRootIdentifier(), root.getDataBefore());
            break;
        case SUBTREE_MODIFIED:
        case WRITE:
            if (root.getDataBefore() != null) {
                removeObject(trans, change.getRootPath().getRootIdentifier(), root.getDataBefore());
            }
            createObject(trans, change.getRootPath().getRootIdentifier(), root.getDataAfter());
            break;
        default:
            throw new IllegalArgumentException("Unhandled modification type " + root.getModificationType());
        }
    }

    private synchronized void initOperationalTopology() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, this.topology,
            new TopologyBuilder().setKey(this.topologyKey).setServerProvided(Boolean.TRUE).setTopologyTypes(this.topologyTypes)
                                 .setLink(Collections.emptyList()).setNode(Collections.emptyList()).build(), true);
        Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize topology {} (transaction {}) by listener {}", AbstractTopologyBuilder.this.topology,
                    trans.getIdentifier(), AbstractTopologyBuilder.this, t);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Destroy the current operational topology data. Note a valid transaction must be provided
     * @throws TransactionCommitFailedException
     */
    private synchronized ListenableFuture<Void> destroyOperationalTopology() {
        requireNonNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        final ListenableFuture<Void> future = trans.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Operational topology removed {}", AbstractTopologyBuilder.this.topology);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Unable to reset operational topology {} (transaction {})",
                    AbstractTopologyBuilder.this.topology, trans.getIdentifier(), t);
            }
        }, MoreExecutors.directExecutor());
        clearTopology();
        return future;
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for topology {}", this);
        Preconditions.checkState(this.chain == null, "Transaction chain has to be closed before being initialized");
        this.chain = this.dataProvider.createTransactionChain(this);
    }

    /**
     * Destroy the current transaction chain
     */
    private synchronized void destroyTransactionChain() {
        if (this.chain != null) {
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
//                LOG.error("Unable to close transaction chain {} for topology builder {}", this.chain, getInstanceIdentifier());
//            }
            this.chain = null;
        }
    }

    /**
     * Reset the data change listener to its initial status
     * By resetting the listener we will be able to recover all the data lost before
     */
    @VisibleForTesting
    protected synchronized void resetListener() {
        requireNonNull(this.listenerRegistration, "Listener on topology " + this + " hasn't been initialized.");
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
     *    rebuilding the whole linkstate topology again and again
     * 2. the #onTransactionChainFailed() normally get invoked after a delay. During that time gap, more
     *    data changes might still be pushed to #onDataTreeChanged(). And because #onTransactionChainFailed()
     *    is not invoked yet, listener restart/transaction chain restart is not done. Thus the new changes
     *    will still cause error and another #onTransactionChainFailed() might be invoked later. The listener
     *    will be restarted again in that case, which is unexpected. Restarting of transaction chain only introduce
     *    little overhead and it's okay to be restarted within a small time window
     *
     * Note: when the listener is restarted, we can disregard all the incoming data changes before the restart is
     * done, as after the listener unregister/reregister, the first #onDataTreeChanged() call will contain the a
     * complete set of existing changes
     *
     * @return if the listener get restarted, return true; otherwise false
     */
    @VisibleForTesting
    protected synchronized boolean restartTransactionChainOnDemand() {
        if (this.listenerScheduledRestartTime > 0) {
            // when the #this.listenerScheduledRestartTime timer timed out we can reset the listener, otherwise we should only reset the transaction chain
            if (System.currentTimeMillis() > this.listenerScheduledRestartTime) {
                // reset the the restart timer
                this.listenerScheduledRestartTime = 0;
                this.listenerScheduledRestartEnforceCounter = 0;
                resetListener();
                return true;
            }

            resetTransactionChain();
        }
        return false;
    }

    @VisibleForTesting
    protected synchronized void scheduleListenerRestart() {
        if (0 == this.listenerScheduledRestartTime) {
            this.listenerScheduledRestartTime = System.currentTimeMillis() + this.listenerResetLimitInMillsec;
        } else if (System.currentTimeMillis() > this.listenerScheduledRestartTime
            && ++this.listenerScheduledRestartEnforceCounter < this.listenerResetEnforceCounter) {
            // if the transaction failure happens again, we will delay the listener restart up to #LISTENER_RESET_LIMIT_IN_MILLSEC times
            this.listenerScheduledRestartTime += this.listenerResetLimitInMillsec;
        }
        LOG.debug("A listener restart was scheduled at {} (current system time is {})", this.listenerScheduledRestartTime, System.currentTimeMillis());
    }

    @Override
    public final synchronized void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Topology builder for {} failed in transaction {}.", getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
        scheduleListenerRestart();
        restartTransactionChainOnDemand();
    }

    @Override
    public final void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("Topology builder for {} shut down", getInstanceIdentifier());
    }
}
