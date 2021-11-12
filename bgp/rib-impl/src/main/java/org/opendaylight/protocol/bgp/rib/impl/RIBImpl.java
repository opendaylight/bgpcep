/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static com.google.common.base.Verify.verifyNotNull;
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
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is thread-safe
public final class RIBImpl extends BGPRibStateImpl implements RIB, DOMTransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
    private static final QName RIB_ID_QNAME = QName.create(Rib.QNAME, "id").intern();

    private final BGPDispatcher dispatcher;
    private final AsNumber localAs;
    private final BgpId bgpIdentifier;
    private final Set<BgpTableType> localTables;
    private final Set<TablesKey> localTablesKeys;
    private final DOMDataBroker domDataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final YangInstanceIdentifier yangRibId;
    private final RIBSupportContextRegistryImpl ribContextRegistry;
    private final CodecsRegistry codecsRegistry;
    private final BGPTableTypeRegistryConsumer tableTypeRegistry;
    private final DOMDataBrokerExtension domService;
    private final Map<DOMTransactionChain, LocRibWriter<?, ?>> txChainToLocRibWriter = new HashMap<>();
    private final Map<TablesKey, RibOutRefresh> vpnTableRefresher = new HashMap<>();
    private final Map<TablesKey, PathSelectionMode> bestPathSelectionStrategies;
    private final RibId ribId;
    private final BGPPeerTracker peerTracker = new BGPPeerTrackerImpl();
    private final BGPRibRoutingPolicy ribPolicies;
    @GuardedBy("this")
    private DOMTransactionChain domChain;
    @GuardedBy("this")
    private boolean isServiceInstantiated;

    public RIBImpl(
            final BGPTableTypeRegistryConsumer tableTypeRegistry,
            final RibId ribId,
            final AsNumber localAs,
            final BgpId localBgpId,
            final RIBExtensionConsumerContext extensions,
            final BGPDispatcher dispatcher,
            final CodecsRegistry codecsRegistry,
            final DOMDataBroker domDataBroker,
            final BGPRibRoutingPolicy ribPolicies,
            final List<BgpTableType> localTables,
            final Map<TablesKey, PathSelectionMode> bestPathSelectionStrategies
    ) {
        super(InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(requireNonNull(ribId))),
                localBgpId, localAs);
        this.tableTypeRegistry = requireNonNull(tableTypeRegistry);
        this.localAs = requireNonNull(localAs);
        bgpIdentifier = requireNonNull(localBgpId);
        this.dispatcher = requireNonNull(dispatcher);

        this.localTables = ImmutableSet.copyOf(localTables);
        // FIXME: can this be immutable?
        localTablesKeys = localTables.stream().
            map(t -> new TablesKey(t.getAfi(), t.getSafi()))
            .collect(Collectors.toCollection(HashSet::new));

        this.domDataBroker = requireNonNull(domDataBroker);
        domService = domDataBroker.getExtensions().get(DOMDataTreeChangeService.class);
        this.extensions = requireNonNull(extensions);
        this.ribPolicies = requireNonNull(ribPolicies);
        this.codecsRegistry = codecsRegistry;
        ribContextRegistry = RIBSupportContextRegistryImpl.create(extensions, codecsRegistry);
        yangRibId = YangInstanceIdentifier.builder().node(BGPRIB_NID).node(RIB_NID)
                .nodeWithKey(Rib.QNAME, RIB_ID_QNAME, ribId.getValue()).build();
        this.bestPathSelectionStrategies = requireNonNull(bestPathSelectionStrategies);
        this.ribId = ribId;

    }

    // FIXME: make this asynchronous?
    private synchronized void startLocRib(final TablesKey key) {
        LOG.debug("Creating LocRib table for {}", key);
        // create locRibWriter for each table
        final DOMDataTreeWriteTransaction tx = domChain.newWriteOnlyTransaction();

        final RIBSupport<? extends Routes, ?> ribSupport = ribContextRegistry.getRIBSupport(key);
        if (ribSupport != null) {
            final MapEntryNode emptyTable = ribSupport.emptyTable();
            final InstanceIdentifierBuilder tableId = YangInstanceIdentifier
                    .builder(yangRibId.node(LOCRIB_NID).node(TABLES_NID)).node(emptyTable.getIdentifier());

            tx.put(LogicalDatastoreType.OPERATIONAL, tableId.build(), emptyTable);
            try {
                tx.commit().get();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.error("Failed to initiate LocRIB for key {}", key, e);
            }
        } else {
            LOG.warn("There's no registered RIB Context for {}", key.getAfi());
        }
    }

    private synchronized <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<? super C>>
            void createLocRibWriter(final TablesKey key) {
        final RIBSupport<C, S> ribSupport = ribContextRegistry.getRIBSupport(key);
        if (ribSupport == null) {
            return;
        }
        LOG.debug("Creating LocRIB writer for key {}", key);
        final DOMTransactionChain txChain = createPeerDOMChain(this);
        PathSelectionMode pathSelectionStrategy = bestPathSelectionStrategies.get(key);
        if (pathSelectionStrategy == null) {
            pathSelectionStrategy = BasePathSelectionModeFactory.createBestPathSelectionStrategy();
        }

        final LocRibWriter<C, S> locRibWriter = LocRibWriter.create(
                ribSupport,
                verifyNotNull(tableTypeRegistry.getAfiSafiType(key)),
                txChain,
                yangRibId,
                localAs,
                getService(),
                ribPolicies,
                peerTracker,
                pathSelectionStrategy);
        vpnTableRefresher.put(key, locRibWriter);
        registerTotalPathCounter(key, locRibWriter);
        registerTotalPrefixesCounter(key, locRibWriter);
        txChainToLocRibWriter.put(txChain, locRibWriter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("bgpId", bgpIdentifier).add("localTables", localTables).toString();
    }

    @Override
    public AsNumber getLocalAs() {
        return localAs;
    }

    @Override
    public BgpId getBgpIdentifier() {
        return bgpIdentifier;
    }

    @Override
    public Set<? extends BgpTableType> getLocalTables() {
        return localTables;
    }

    @Override
    public BGPDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public synchronized void onTransactionChainFailed(final DOMTransactionChain chain,
            final DOMDataTreeTransaction transaction, final Throwable cause) {
        LOG.error("Broken chain in RIB {} transaction {}",
            getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
        final LocRibWriter<?, ?> locRibWriter = txChainToLocRibWriter.remove(chain);
        if (locRibWriter != null) {
            final DOMTransactionChain newChain = createPeerDOMChain(this);
            startLocRib(locRibWriter.getTableKey());
            locRibWriter.restart(newChain);
            txChainToLocRibWriter.put(newChain, locRibWriter);
        }
    }

    @Override
    public void onTransactionChainSuccessful(final DOMTransactionChain chain) {
        LOG.info("RIB {} closed successfully", getInstanceIdentifier());
    }

    @Override
    public Set<TablesKey> getLocalTablesKeys() {
        return localTablesKeys;
    }

    @Override
    public boolean supportsTable(final TablesKey tableKey) {
        return localTablesKeys.contains(tableKey);
    }

    @Override
    public BGPRibRoutingPolicy getRibPolicies() {
        return ribPolicies;
    }

    @Override
    public BGPPeerTracker getPeerTracker() {
        return peerTracker;
    }

    @Override
    public void refreshTable(final TablesKey tk, final PeerId peerId) {
        final RibOutRefresh table = vpnTableRefresher.get(tk);
        if (table != null) {
            table.refreshTable(tk, peerId);
        }
    }

    @Override
    public DOMDataTreeChangeService getService() {
        return (DOMDataTreeChangeService) domService;
    }

    @Override
    public YangInstanceIdentifier getYangRibId() {
        return yangRibId;
    }

    @Override
    public DOMTransactionChain createPeerDOMChain(final DOMTransactionChainListener listener) {
        return domDataBroker.createMergingTransactionChain(listener);
    }

    @Override
    public RIBExtensionConsumerContext getRibExtensions() {
        return extensions;
    }

    @Override
    public RIBSupportContextRegistry getRibSupportContext() {
        return ribContextRegistry;
    }

    @Override
    public CodecsRegistry getCodecsRegistry() {
        return codecsRegistry;
    }

    public synchronized void instantiateServiceInstance() {
        isServiceInstantiated = true;
        setActive(true);
        domChain = domDataBroker.createMergingTransactionChain(this);
        LOG.debug("Instantiating RIB table {} at {}", ribId, yangRibId);

        final ContainerNode bgpRib = Builders.containerBuilder().withNodeIdentifier(BGPRIB_NID)
                .addChild(ImmutableNodes.mapNodeBuilder(RIB_NID).build()).build();

        final MapEntryNode ribInstance = Builders.mapEntryBuilder().withNodeIdentifier(
                NodeIdentifierWithPredicates.of(Rib.QNAME, RIB_ID_QNAME, ribId.getValue()))
                .addChild(ImmutableNodes.leafNode(RIB_ID_QNAME, ribId.getValue()))
                .addChild(ImmutableNodes.mapNodeBuilder(PEER_NID).build())
                .addChild(Builders.containerBuilder().withNodeIdentifier(LOCRIB_NID)
                        .addChild(ImmutableNodes.mapNodeBuilder(TABLES_NID).build())
                        .build()).build();

        final DOMDataTreeWriteTransaction trans = domChain.newWriteOnlyTransaction();

        // merge empty BgpRib + Rib, to make sure the top-level parent structure is present
        trans.merge(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.create(BGPRIB_NID), bgpRib);
        trans.put(LogicalDatastoreType.OPERATIONAL, yangRibId, ribInstance);

        try {
            trans.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Failed to initiate RIB {}", yangRibId, e);
        }

        LOG.debug("Effective RIB created.");

        localTablesKeys.forEach(this::startLocRib);
        localTablesKeys.forEach(this::createLocRibWriter);
    }

    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (!isServiceInstantiated) {
            LOG.trace("RIB {} already closed", ribId.getValue());
            return CommitInfo.emptyFluentFuture();
        }
        LOG.info("Close RIB {}", ribId.getValue());
        isServiceInstantiated = false;
        setActive(false);

        txChainToLocRibWriter.values().forEach(LocRibWriter::close);
        txChainToLocRibWriter.clear();

        final DOMDataTreeWriteTransaction t = domChain.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, getYangRibId());
        final FluentFuture<? extends CommitInfo> cleanFuture = t.commit();
        cleanFuture.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("RIB cleaned {}", ribId.getValue());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to clean RIB {}",
                        ribId.getValue(), throwable);
            }
        }, MoreExecutors.directExecutor());
        domChain.close();
        return cleanFuture;
    }
}
