/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTopologyBuilder<T extends Route> implements AutoCloseable, DataTreeChangeListener<T>, TopologyReference, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyBuilder.class);
    // we limit the listener reset interval to be 5 min at most
    private static final long LISTENER_RESET_LIMIT_IN_MILLSEC = 5 * 60 * 1000;
    private final InstanceIdentifier<Topology> topology;
    private final RibReference locRibReference;
    private final DataBroker dataProvider;
    private final Class<? extends AddressFamily> afi;
    private final Class<? extends SubsequentAddressFamily> safi;
    private final TopologyKey topologyKey;
    private final TopologyTypes topologyTypes;

    @GuardedBy("this")
    private ListenerRegistration<AbstractTopologyBuilder<T>> listenerRegistration = null;
    @GuardedBy("this")
    private BindingTransactionChain chain = null;
    @GuardedBy("this")
    private boolean closed = false;
    @GuardedBy("this")
    private long lastListenerRegTimestamp = 0;
    @GuardedBy("this")
    private boolean transChainLocked = false;

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes types, final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        this.dataProvider = dataProvider;
        this.locRibReference = Preconditions.checkNotNull(locRibReference);
        this.topologyKey = new TopologyKey(Preconditions.checkNotNull(topologyId));
        this.topologyTypes = types;
        this.afi = afi;
        this.safi = safi;
        this.topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, this.topologyKey).build();

        LOG.debug("Initiating topology builder from {} at {}. AFI={}, SAFI={}", locRibReference, this.topology, this.afi, this.safi);
        initTransactionChain();
        initOperationalTopology();
        registerDataChangeListener();
    }

    @Deprecated
    public final InstanceIdentifier<Tables> tableInstanceIdentifier(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        return this.locRibReference.getInstanceIdentifier().builder().child(LocRib.class).child(Tables.class, new TablesKey(afi, safi)).build();
    }

    /**
     * Register to data tree change listener
     */
    private synchronized void registerDataChangeListener() {
        Preconditions.checkState(this.listenerRegistration == null, "Topology Listener on topology %s has been registered before.", this.getInstanceIdentifier());
        final InstanceIdentifier<Tables> tablesId = this.locRibReference.getInstanceIdentifier().child(LocRib.class).child(Tables.class, new TablesKey(this.afi, this.safi));
        final DataTreeIdentifier<T> id = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getRouteWildcard(tablesId));

        this.listenerRegistration = this.dataProvider.registerDataTreeChangeListener(id, this);
        this.lastListenerRegTimestamp = System.currentTimeMillis();
        LOG.debug("Registered listener {} on topology {}. Timestamp={}", this, this.getInstanceIdentifier(), this.lastListenerRegTimestamp);
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

    @Override
    public final InstanceIdentifier<Topology> getInstanceIdentifier() {
        return this.topology;
    }

    @Override
    public final synchronized void close() throws TransactionCommitFailedException {
        if (this.closed) {
            LOG.trace("Transaction chain was already closed.");
            return;
        }
        this.closed = true;
        LOG.info("Shutting down builder for {}", getInstanceIdentifier());
        unregisterDataChangeListener();
        destroyOperationalTopology();
        destroyTransactionChain();
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<T>> changes) {
        if (this.closed) {
            LOG.trace("Transaction chain was already closed, skipping update.");
            return;
        }
        if (this.transChainLocked) {
            LOG.debug("Transaction chain was locked due to exception. Skip updates until the chain is reset");
            return;
        }
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        LOG.debug("Received data change {} event with transaction {}", changes, trans.getIdentifier());
        for (final DataTreeModification<T> change : changes) {
            try {
                routeChanged(change, trans);
            } catch (final RuntimeException e) {
                LOG.warn("Data change {} (transaction {}) was not completely propagated to listener {}", change, trans.getIdentifier(), this, e);
                // trans.cancel() is not supported by PingPongTransactionChain, so we just skip the problematic change
                // trans.submit() must be called first to unlock the current transaction chain, to make the chain closable
                // so we cannot exit the #onDataTreeChanged() yet
                lockTransactionChain();
                break;
            }
        }
        Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to propagate change (transaction {}) by listener {}", trans.getIdentifier(), AbstractTopologyBuilder.this, t);
            }
        });
    }

    private void routeChanged(final DataTreeModification<T> change, final ReadWriteTransaction trans) {
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
        Preconditions.checkNotNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.put(LogicalDatastoreType.OPERATIONAL, this.topology,
            new TopologyBuilder().setKey(this.topologyKey).setServerProvided(Boolean.TRUE).setTopologyTypes(this.topologyTypes)
                                 .setLink(Collections.<Link>emptyList()).setNode(Collections.<Node>emptyList()).build(), true);
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
        });
    }

    /**
     * Destroy the current operational topology data. Note a valid transaction must be provided
     * @throws TransactionCommitFailedException
     */
    private synchronized void destroyOperationalTopology() {
        Preconditions.checkNotNull(this.chain, "A valid transaction chain must be provided.");
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        try {
            trans.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Unable to reset operational topology {} (transaction {})", this.topology, trans.getIdentifier(), e);
        }
    }

    /**
     * Reset a transaction chain by closing the current chain and starting a new one
     */
    private synchronized void initTransactionChain() {
        LOG.debug("Initializing transaction chain for topology {}", this);
        Preconditions.checkState(this.chain == null, "Transaction chain has to be closed before being initialized");
        this.chain = this.dataProvider.createTransactionChain(this);
        // unlock the transaction chain
        this.transChainLocked = false;
    }

    /**
     * Destroy the current transaction chain
     */
    private synchronized void destroyTransactionChain() {
        if (this.chain != null) {
            LOG.debug("Destroy transaction chain for topology {}", this);
            try {
                this.chain.close();
            } catch (Exception e) {
                // exception may happen when we try to close a locked transaction chain (chain with transaction not submitted)
                LOG.warn("Failed to close transaction chain {}. Abandon the chain directly..", this.chain, e);
            }
            // abandon the chain anyway
            this.chain = null;
        }
    }

    /**
     * Reset the data change listener to its initial status
     * By resetting the listener we will be able to recover all the data lost before
     */
    private synchronized void resetListener() {
        Preconditions.checkNotNull(this.listenerRegistration, "Listener on topology %s hasn't been initialized.", this);
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
    private synchronized void resetTransactionChain() {
        destroyTransactionChain();
        initTransactionChain();
    }

    /**
     * Lock the transaction chain to prevent further transaction to be submitted before the chain get reset
     * Since the transaction works in async manner, new transactions could still be submitted to previous bad transaction chain
     */
    private synchronized void lockTransactionChain() {
        this.transChainLocked = true;
    }

    @Override
    public final synchronized void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Topology builder for {} failed in transaction {}. Restarting listener", getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
        lockTransactionChain();
        // we should avoid restarting the whole listener as there might be huge overhead when the topology is big
        // when the reset timer timed out we can reset the listener, otherwise we should only reset the transaction chain
        if (System.currentTimeMillis() - this.lastListenerRegTimestamp > LISTENER_RESET_LIMIT_IN_MILLSEC) {
            // reset the listener as we are unable to cancel the transaction chain properly
            resetListener();
        } else {
            resetTransactionChain();
        }
    }

    @Override
    public final void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("Topology builder for {} shut down", getInstanceIdentifier());
    }
}
