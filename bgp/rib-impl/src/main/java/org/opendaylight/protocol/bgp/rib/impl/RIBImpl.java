/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.BGPRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.LOCRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.RIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RibOutRefresh;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPRibStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpId;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is thread-safe
public final class RIBImpl extends BGPRibStateImpl implements RIB, TransactionChainListener,
        DOMTransactionChainListener, SchemaContextListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
    private static final QName RIB_ID_QNAME = QName.create(Rib.QNAME, "id").intern();

    private final BGPDispatcher dispatcher;
    private final AsNumber localAs;
    private final BgpId bgpIdentifier;
    private final Set<BgpTableType> localTables;
    private final Set<TablesKey> localTablesKeys;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final YangInstanceIdentifier yangRibId;
    private final RIBSupportContextRegistryImpl ribContextRegistry;
    private final CodecsRegistryImpl codecsRegistry;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final DOMDataBrokerExtension domService;
    private final Map<TransactionChain, LocRibWriter> txChainToLocRibWriter = new HashMap<>();
    private final Map<TablesKey, PathSelectionMode> bestPathSelectionStrategies;
    private final RibId ribId;
    private final BGPPeerTracker peerTracker = new BGPPeerTrackerImpl();
    private final BGPRibRoutingPolicy ribPolicies;
    @GuardedBy("this")
    private ClusterSingletonServiceRegistration registration;
    @GuardedBy("this")
    private DOMTransactionChain domChain;
    @GuardedBy("this")
    private boolean isServiceInstantiated;
    private final Map<TablesKey, RibOutRefresh> vpnTableRefresher = new HashMap<>();

    public RIBImpl(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final RibId ribId,
            final AsNumber localAs,
            final BgpId localBgpId,
            final RIBExtensionConsumerContext extensions,
            final BGPDispatcher dispatcher,
            final CodecsRegistryImpl codecsRegistry,
            final DOMDataBroker domDataBroker,
            final DataBroker dataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final List<BgpTableType> localTables,
            final Map<TablesKey, PathSelectionMode> bestPathSelectionStrategies
    ) {
        super(InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(requireNonNull(ribId))),
                localBgpId, localAs);
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.localAs = requireNonNull(localAs);
        this.bgpIdentifier = requireNonNull(localBgpId);
        this.dispatcher = requireNonNull(dispatcher);
        this.localTables = ImmutableSet.copyOf(localTables);
        this.localTablesKeys = new HashSet<>();
        this.domDataBroker = requireNonNull(domDataBroker);
        this.dataBroker = requireNonNull(dataBroker);
        this.domService = this.domDataBroker.getExtensions().get(DOMDataTreeChangeService.class);
        this.extensions = requireNonNull(extensions);
        this.ribPolicies = requireNonNull(ribPolicies);
        this.codecsRegistry = codecsRegistry;
        this.ribContextRegistry = RIBSupportContextRegistryImpl.create(extensions, this.codecsRegistry);
        this.yangRibId = YangInstanceIdentifier.builder().node(BGPRIB_NID).node(RIB_NID)
                .nodeWithKey(Rib.QNAME, RIB_ID_QNAME, ribId.getValue()).build();
        this.bestPathSelectionStrategies = requireNonNull(bestPathSelectionStrategies);
        this.ribId = ribId;

        for (final BgpTableType t : this.localTables) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());
            this.localTablesKeys.add(key);
        }
    }

    private synchronized void startLocRib(final TablesKey key) {
        LOG.debug("Creating LocRib table for {}", key);
        // create locRibWriter for each table
        final DOMDataTreeWriteTransaction tx = this.domChain.newWriteOnlyTransaction();

        final RIBSupport<? extends Routes, ?, ?, ?> ribSupport = this.ribContextRegistry.getRIBSupport(key);
        if (ribSupport != null) {
            final MapEntryNode emptyTable = ribSupport.emptyTable();
            final InstanceIdentifierBuilder tableId = YangInstanceIdentifier
                    .builder(this.yangRibId.node(LOCRIB_NID).node(TABLES_NID)).node(emptyTable.getIdentifier());

            tx.put(LogicalDatastoreType.OPERATIONAL, tableId.build(), emptyTable);
            try {
                tx.commit().get();
            } catch (final InterruptedException | ExecutionException e1) {
                LOG.error("Failed to initiate LocRIB for key {}", key, e1);
            }
        } else {
            LOG.warn("There's no registered RIB Context for {}", key.getAfi());
        }
    }

    private synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>,
            R extends Route & ChildOf<? super S> & Identifiable<I>, I extends Identifier<R>>
            void createLocRibWriter(final TablesKey key) {
        final RIBSupport<C, S, R, I> ribSupport = this.ribContextRegistry.getRIBSupport(key);
        if (ribSupport == null) {
            return;
        }
        LOG.debug("Creating LocRIB writer for key {}", key);
        final TransactionChain txChain = createPeerChain(this);
        PathSelectionMode pathSelectionStrategy = this.bestPathSelectionStrategies.get(key);
        if (pathSelectionStrategy == null) {
            pathSelectionStrategy = BasePathSelectionModeFactory.createBestPathSelectionStrategy();
        }

        final LocRibWriter<C, S, R, I> locRibWriter = LocRibWriter.create(
                ribSupport,
                this.tableTypeRegistry.getAfiSafiType(key).get(),
                txChain,
                getInstanceIdentifier(),
                this.localAs,
                getDataBroker(),
                this.ribPolicies,
                this.peerTracker,
                pathSelectionStrategy);
        this.vpnTableRefresher.put(key, locRibWriter);
        registerTotalPathCounter(key, locRibWriter);
        registerTotalPrefixesCounter(key, locRibWriter);
        this.txChainToLocRibWriter.put(txChain, locRibWriter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bgpId", bgpIdentifier).add("localTables", localTables).toString();
    }

    @Override
    public synchronized void close() {
        if (this.registration != null) {
            this.registration.close();
            this.registration = null;
        }
    }

    @Override
    public AsNumber getLocalAs() {
        return this.localAs;
    }

    @Override
    public BgpId getBgpIdentifier() {
        return this.bgpIdentifier;
    }

    @Override
    public Set<? extends BgpTableType> getLocalTables() {
        return this.localTables;
    }

    @Override
    public BGPDispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain chain,
            final Transaction transaction, final Throwable cause) {
        LOG.error("Broken chain in RIB {} transaction {}",
                getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
        if (this.txChainToLocRibWriter.containsKey(chain)) {
            final LocRibWriter locRibWriter = this.txChainToLocRibWriter.remove(chain);
            final TransactionChain newChain = createPeerChain(this);
            startLocRib(locRibWriter.getTableKey());
            locRibWriter.restart(newChain);
            this.txChainToLocRibWriter.put(newChain, locRibWriter);
        }
    }

    @Override
    public synchronized void onTransactionChainFailed(final DOMTransactionChain chain,
            final DOMDataTreeTransaction transaction, final Throwable cause) {
        LOG.error("Broken chain in RIB {} transaction {}",
            getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain chain) {
        LOG.info("RIB {} closed successfully", getInstanceIdentifier());
    }

    @Override
    public void onTransactionChainSuccessful(final DOMTransactionChain chain) {
        LOG.info("RIB {} closed successfully", getInstanceIdentifier());
    }

    @Override
    public Set<TablesKey> getLocalTablesKeys() {
        return this.localTablesKeys;
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return this.localTablesKeys.contains(tableKey);
    }

    @Override
    public BGPRibRoutingPolicy getRibPolicies() {
        return this.ribPolicies;
    }

    @Override
    public BGPPeerTracker getPeerTracker() {
        return this.peerTracker;
    }

    @Override
    public void refreshTable(final TablesKey tk, final PeerId peerId) {
        final RibOutRefresh table = this.vpnTableRefresher.get(tk);
        if (table != null) {
            table.refreshTable(tk, peerId);
        }
    }

    @Override
    public DOMDataTreeChangeService getService() {
        return (DOMDataTreeChangeService) this.domService;
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public YangInstanceIdentifier getYangRibId() {
        return this.yangRibId;
    }

    @Override
    public TransactionChain createPeerChain(final TransactionChainListener listener) {
        return this.dataBroker.createMergingTransactionChain(listener);
    }

    @Override
    public DOMTransactionChain createPeerDOMChain(final DOMTransactionChainListener listener) {
        return this.domDataBroker.createMergingTransactionChain(listener);
    }

    @Override
    public RIBExtensionConsumerContext getRibExtensions() {
        return this.extensions;
    }

    @Override
    public RIBSupportContextRegistry getRibSupportContext() {
        return this.ribContextRegistry;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext context) {
        this.codecsRegistry.onSchemaContextUpdated(context);
    }

    @Override
    public CodecsRegistry getCodecsRegistry() {
        return this.codecsRegistry;
    }

    public synchronized void instantiateServiceInstance() {
        this.isServiceInstantiated = true;
        setActive(true);
        this.domChain = this.domDataBroker.createMergingTransactionChain(this);
        LOG.debug("Instantiating RIB table {} at {}", this.ribId, this.yangRibId);

        final ContainerNode bgpRib = Builders.containerBuilder().withNodeIdentifier(BGPRIB_NID)
                .addChild(ImmutableNodes.mapNodeBuilder(RIB_NID).build()).build();

        final MapEntryNode ribInstance = Builders.mapEntryBuilder().withNodeIdentifier(
                new NodeIdentifierWithPredicates(Rib.QNAME, RIB_ID_QNAME, this.ribId.getValue()))
                .addChild(ImmutableNodes.leafNode(RIB_ID_QNAME, this.ribId.getValue()))
                .addChild(ImmutableNodes.mapNodeBuilder(PEER_NID).build())
                .addChild(Builders.containerBuilder().withNodeIdentifier(LOCRIB_NID)
                        .addChild(ImmutableNodes.mapNodeBuilder(TABLES_NID).build())
                        .build()).build();

        final DOMDataTreeWriteTransaction trans = this.domChain.newWriteOnlyTransaction();

        // merge empty BgpRib + Rib, to make sure the top-level parent structure is present
        trans.merge(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.create(BGPRIB_NID), bgpRib);
        trans.put(LogicalDatastoreType.OPERATIONAL, this.yangRibId, ribInstance);

        try {
            trans.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Failed to initiate RIB {}", this.yangRibId, e);
        }

        LOG.debug("Effective RIB created.");

        this.localTablesKeys.forEach(this::startLocRib);
        this.localTablesKeys.forEach(this::createLocRibWriter);
    }

    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (!this.isServiceInstantiated) {
            LOG.trace("RIB {} already closed", this.ribId.getValue());
            return CommitInfo.emptyFluentFuture();
        }
        LOG.info("Close RIB {}", this.ribId.getValue());
        this.isServiceInstantiated = false;
        setActive(false);

        this.txChainToLocRibWriter.values().forEach(LocRibWriter::close);
        this.txChainToLocRibWriter.clear();

        final DOMDataTreeWriteTransaction t = this.domChain.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, getYangRibId());
        final FluentFuture<? extends CommitInfo> cleanFuture = t.commit();
        cleanFuture.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("RIB cleaned {}", RIBImpl.this.ribId.getValue());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to clean RIB {}",
                        RIBImpl.this.ribId.getValue(), throwable);
            }
        }, MoreExecutors.directExecutor());
        this.domChain.close();
        return cleanFuture;
    }
}
