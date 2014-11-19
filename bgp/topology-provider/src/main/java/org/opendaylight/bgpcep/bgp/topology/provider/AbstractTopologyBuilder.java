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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTopologyBuilder<T extends Route> implements AutoCloseable, DataChangeListener, LocRIBListener, TopologyReference, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyBuilder.class);
    private final InstanceIdentifier<Topology> topology;
    private final BindingTransactionChain chain;
    private final RibReference locRibReference;
    private final Class<T> idClass;

    @GuardedBy("this")
    private boolean closed = false;

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes types, final Class<T> idClass) {
        this.locRibReference = Preconditions.checkNotNull(locRibReference);
        this.idClass = Preconditions.checkNotNull(idClass);
        this.chain = dataProvider.createTransactionChain(this);

        final TopologyKey tk = new TopologyKey(Preconditions.checkNotNull(topologyId));
        this.topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, tk).toInstance();

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

    public final InstanceIdentifier<Tables> tableInstanceIdentifier(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        return this.locRibReference.getInstanceIdentifier().builder().child(LocRib.class).child(Tables.class, new TablesKey(afi, safi)).toInstance();
    }

    protected abstract void createObject(ReadWriteTransaction trans, InstanceIdentifier<T> id, T value);

    protected abstract void removeObject(ReadWriteTransaction trans, InstanceIdentifier<T> id, T value);

    @Override
    public final InstanceIdentifier<Topology> getInstanceIdentifier() {
        return this.topology;
    }

    private void addIdentifier(final InstanceIdentifier<?> i, final String set, final Set<InstanceIdentifier<T>> out) {
        final InstanceIdentifier<T> id = i.firstIdentifierOf(this.idClass);
        if (id != null) {
            out.add(id);
        } else {
            LOG.debug("Identifier {} in {} set does not contain listening class {}, ignoring it", i, set, this.idClass);
        }
    }

    @Override
    public synchronized final void onLocRIBChange(final ReadWriteTransaction trans,
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        LOG.debug("Received data change {} event with transaction {}", event, trans.getIdentifier());
        if (this.closed) {
            LOG.trace("Transaction chain was already closed, skipping update.");
            return;
        }

        // FIXME: speed this up
        final Set<InstanceIdentifier<T>> ids = new HashSet<>();
        for (final InstanceIdentifier<?> i : event.getRemovedPaths()) {
            addIdentifier(i, "remove", ids);
        }
        for (final InstanceIdentifier<?> i : event.getUpdatedData().keySet()) {
            addIdentifier(i, "update", ids);
        }
        for (final InstanceIdentifier<?> i : event.getCreatedData().keySet()) {
            addIdentifier(i, "create", ids);
        }

        final Map<InstanceIdentifier<?>, ? extends DataObject> o = event.getOriginalData();
        final Map<InstanceIdentifier<?>, DataObject> u = event.getUpdatedData();
        final Map<InstanceIdentifier<?>, DataObject> c = event.getCreatedData();
        for (final InstanceIdentifier<T> i : ids) {
            final T oldValue = this.idClass.cast(o.get(i));
            T newValue = this.idClass.cast(u.get(i));
            if (newValue == null) {
                newValue = this.idClass.cast(c.get(i));
            }

            LOG.debug("Updating object {} value {} -> {}", i, oldValue, newValue);
            if (oldValue != null) {
                removeObject(trans, i, oldValue);
            }
            if (newValue != null) {
                createObject(trans, i, newValue);
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

    @Override
    public synchronized final void close() throws TransactionCommitFailedException {
        LOG.info("Shutting down builder for {}", getInstanceIdentifier());
        final WriteTransaction trans = this.chain.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        trans.submit().checkedGet();
        this.chain.close();
        this.closed = true;
    }

    @Override
    public final void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final ReadWriteTransaction trans = this.chain.newReadWriteTransaction();

        try {
            onLocRIBChange(trans, change);
        } catch (final RuntimeException e) {
            LOG.warn("Data change {} was not completely propagated to listener {}", change, this, e);
            return;
        }
    }

    @Override
    public final void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        // TODO: restart?
        LOG.error("Topology builder for {} failed in transaction {}", getInstanceIdentifier(), transaction.getIdentifier(), cause);
    }

    @Override
    public final void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("Topology builder for {} shut down", getInstanceIdentifier());
    }
}
