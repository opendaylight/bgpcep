/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTopologyBuilder<T extends Route> implements AutoCloseable, DataChangeListener, LocRIBListener, TopologyReference {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyBuilder.class);
    private final RibReference locRibReference;
    private final InstanceIdentifier<Topology> topology;
    private final DataBroker dataProvider;
    private final Class<T> idClass;

    protected AbstractTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final TopologyTypes types, final Class<T> idClass) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
        this.locRibReference = Preconditions.checkNotNull(locRibReference);
        this.idClass = Preconditions.checkNotNull(idClass);

        final TopologyKey tk = new TopologyKey(Preconditions.checkNotNull(topologyId));
        this.topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, tk).toInstance();

        LOG.debug("Initiating topology builder from {} at {}", locRibReference, this.topology);

        final ReadWriteTransaction t = dataProvider.newReadWriteTransaction();
        final Optional<Topology> o;
        try {
            o = t.read(LogicalDatastoreType.OPERATIONAL, this.topology).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to read topology " + topology, e);
        }
        Preconditions.checkState(!o.isPresent(), "Data provider conflict detected on object {}", this.topology);

        t.put(LogicalDatastoreType.OPERATIONAL, this.topology,
                new TopologyBuilder().setKey(tk).setServerProvided(Boolean.TRUE).setTopologyTypes(types).build());
        Futures.addCallback(t.commit(), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Change committed successfully");
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

    public final DataBroker getDataProvider() {
        return this.dataProvider;
    }

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
    public final void onLocRIBChange(final ReadWriteTransaction trans,
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        LOG.debug("Received data change {} event with transaction {}", event, trans);

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
        final Map<InstanceIdentifier<?>, DataObject> n = event.getUpdatedData();
        for (final InstanceIdentifier<T> i : ids) {
            final T oldValue = this.idClass.cast(o.get(i));
            final T newValue = this.idClass.cast(n.get(i));

            LOG.debug("Updating object {} value {} -> {}", i, oldValue, newValue);
            if (oldValue != null) {
                removeObject(trans, i, oldValue);
            }
            if (newValue != null) {
                createObject(trans, i, newValue);
            }
        }

        Futures.addCallback(trans.commit(), new FutureCallback<RpcResult<TransactionStatus>>() {
            @Override
            public void onSuccess(final RpcResult<TransactionStatus> result) {
                LOG.trace("Change committed successfully");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to propagate change by listener {}", AbstractTopologyBuilder.this);
            }
        });
    }

    @Override
    public final void close() throws InterruptedException, ExecutionException {
        LOG.info("Shutting down builder for {}", getInstanceIdentifier());
        final WriteTransaction trans = this.dataProvider.newWriteOnlyTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier());
        trans.commit().get();
    }

    @Override
    public final void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final ReadWriteTransaction trans = this.dataProvider.newReadWriteTransaction();

        try {
            onLocRIBChange(trans, change);
        } catch (final RuntimeException e) {
            LOG.warn("Data change {} was not completely propagated to listener {}", change, this, e);
            return;
        }

        //        switch (trans.getStatus()) {
        //        case COMMITED:
        //        case SUBMITED:
        //            break;
        //        case NEW:
        //            LOG.warn("Data change {} transaction {} was not committed by builder {}", change, trans, this);
        //            break;
        //        case CANCELED:
        //        case FAILED:
        //            LOG.error("Data change {} transaction {} failed to commit", change, trans);
        //            break;
        //        }
    }
}
