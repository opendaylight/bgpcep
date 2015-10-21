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
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeService;
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
    private final InstanceIdentifier<Topology> topology;
    private final BindingTransactionChain chain;
    private final RibReference locRibReference;

    @GuardedBy("this")
    private boolean closed = false;

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes types) {
        this.locRibReference = Preconditions.checkNotNull(locRibReference);
        this.chain = dataProvider.createTransactionChain(this);

        final TopologyKey tk = new TopologyKey(Preconditions.checkNotNull(topologyId));
        this.topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, tk).build();

        LOG.debug("Initiating topology builder from {} at {}", locRibReference, this.topology);

        final WriteTransaction t = this.chain.newWriteOnlyTransaction();

        t.put(LogicalDatastoreType.OPERATIONAL, this.topology,
                new TopologyBuilder().setKey(tk).setServerProvided(Boolean.TRUE).setTopologyTypes(types)
                    .setLink(Collections.<Link>emptyList()).setNode(Collections.<Node>emptyList()).build(), true);
        Futures.addCallback(t.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Transaction {} committed successfully", t.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initiate topology {} by listener {}", AbstractTopologyBuilder.this.topology,
                        AbstractTopologyBuilder.this, t);
            }
        });
    }

    @Deprecated
    public final InstanceIdentifier<Tables> tableInstanceIdentifier(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        return this.locRibReference.getInstanceIdentifier().builder().child(LocRib.class).child(Tables.class, new TablesKey(afi, safi)).build();
    }

    public final ListenerRegistration<AbstractTopologyBuilder<T>> start(final DataTreeChangeService service, final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        final InstanceIdentifier<Tables> tablesId = this.locRibReference.getInstanceIdentifier().child(LocRib.class).child(Tables.class, new TablesKey(afi, safi));
        final DataTreeIdentifier<T> id = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getRouteWildcard(tablesId));

        return service.registerDataTreeChangeListener(id, this);
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
        LOG.info("Shutting down builder for {}", getInstanceIdentifier());
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        trans.submit().checkedGet();
        this.chain.close();
        this.closed = true;
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<T>> changes) {
        if (this.closed) {
            LOG.trace("Transaction chain was already closed, skipping update.");
            return;
        }
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();
        LOG.debug("Received data change {} event with transaction {}", changes, trans.getIdentifier());
        for (final DataTreeModification<T> change : changes) {
            try {
                routeChanged(change, trans);
            } catch (final RuntimeException e) {
                LOG.warn("Data change {} was not completely propagated to listener {}, aborting", change, this, e);
                trans.cancel();
                return;
            }
        }
        Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Transaction {} committed successfully", trans.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to propagate change by listener {}", AbstractTopologyBuilder.this);
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

    @Override
    public final void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        // TODO: restart?
        LOG.error("Topology builder for {} failed in transaction {}", getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public final void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("Topology builder for {} shut down", getInstanceIdentifier());
    }
}
