/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.impl.state.BGPRIBStateImpl;
import org.opendaylight.protocol.bgp.rib.spi.ExportPolicyPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.util.ClusterSingletonServiceRegistrationHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public final class RIBImpl extends BGPRIBStateImpl implements ClusterSingletonService, RIB, TransactionChainListener,
        SchemaContextListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
    private static final QName RIB_ID_QNAME = QName.create(Rib.QNAME, "id").intern();
    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Attributes.QNAME);

    private final BGPDispatcher dispatcher;
    private final AsNumber localAs;
    private final BgpId bgpIdentifier;
    private final Set<BgpTableType> localTables;
    private final Set<TablesKey> localTablesKeys;
    private final DOMDataBroker domDataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final YangInstanceIdentifier yangRibId;
    private final RIBSupportContextRegistryImpl ribContextRegistry;
    private final CodecsRegistryImpl codecsRegistry;
    private final ServiceGroupIdentifier serviceGroupIdentifier;
    private final ClusterSingletonServiceProvider provider;
    private final DOMDataBrokerExtension service;
    private final Map<TransactionChain<?, ?>, LocRibWriter> txChainToLocRibWriter = new HashMap<>();
    private final Map<TablesKey, PathSelectionMode> bestPathSelectionStrategies;
    private final ImportPolicyPeerTracker importPolicyPeerTracker;
    private final RibId ribId;
    private final Map<TablesKey, ExportPolicyPeerTracker> exportPolicyPeerTrackerMap;
    private ClusterSingletonServiceRegistration registration;
    private DOMTransactionChain domChain;
    @GuardedBy("this")
    private boolean isServiceInstantiated;

    public RIBImpl(final ClusterSingletonServiceProvider provider, final RibId ribId, final AsNumber localAs, final BgpId localBgpId,
            final ClusterIdentifier clusterId, final RIBExtensionConsumerContext extensions, final BGPDispatcher dispatcher,
            final BindingCodecTreeFactory codecFactory, final DOMDataBroker domDataBroker, final List<BgpTableType> localTables,
            @Nonnull final Map<TablesKey, PathSelectionMode> bestPathSelectionStrategies,
            final GeneratedClassLoadingStrategy classStrategy) {
        super(InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(requireNonNull(ribId))),
                localBgpId, localAs);
        this.localAs = requireNonNull(localAs);
        this.bgpIdentifier = requireNonNull(localBgpId);
        this.dispatcher = requireNonNull(dispatcher);
        this.localTables = ImmutableSet.copyOf(localTables);
        this.localTablesKeys = new HashSet<>();
        this.domDataBroker = requireNonNull(domDataBroker);
        this.service = this.domDataBroker.getSupportedExtensions().get(DOMDataTreeChangeService.class);
        this.extensions = requireNonNull(extensions);
        this.codecsRegistry = CodecsRegistryImpl.create(codecFactory, classStrategy);
        this.ribContextRegistry = RIBSupportContextRegistryImpl.create(extensions, this.codecsRegistry);
        final InstanceIdentifierBuilder yangRibIdBuilder = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Rib.QNAME);
        this.yangRibId = yangRibIdBuilder.nodeWithKey(Rib.QNAME, RIB_ID_QNAME, ribId.getValue()).build();
        this.bestPathSelectionStrategies = requireNonNull(bestPathSelectionStrategies);
        final ClusterIdentifier cId = clusterId == null ? new ClusterIdentifier(localBgpId) : clusterId;
        this.ribId = ribId;
        final PolicyDatabase policyDatabase = new PolicyDatabase(this.localAs.getValue(), localBgpId, cId);
        this.importPolicyPeerTracker = new ImportPolicyPeerTrackerImpl(policyDatabase);
        this.serviceGroupIdentifier = ServiceGroupIdentifier.create(this.ribId.getValue() + "-service-group");
        requireNonNull(provider, "ClusterSingletonServiceProvider is null");
        this.provider = provider;

        final ImmutableMap.Builder<TablesKey, ExportPolicyPeerTracker> exportPolicies = new ImmutableMap.Builder<>();
        for (final BgpTableType t : this.localTables) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());
            this.localTablesKeys.add(key);
            exportPolicies.put(key, new ExportPolicyPeerTrackerImpl(policyDatabase, key));
        }
        this.exportPolicyPeerTrackerMap = exportPolicies.build();
        LOG.info("RIB Singleton Service {} registered, RIB {}", getIdentifier().getValue(), this.ribId.getValue());
        //this need to be always the last step
        this.registration = registerClusterSingletonService(this);
    }

    private void startLocRib(final TablesKey key) {
        LOG.debug("Creating LocRib table for {}", key);
        // create locRibWriter for each table
        final DOMDataWriteTransaction tx = this.domChain.newWriteOnlyTransaction();

        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> table = ImmutableNodes.mapEntryBuilder();
        table.withNodeIdentifier(RibSupportUtils.toYangTablesKey(key));
        table.withChild(EMPTY_TABLE_ATTRIBUTES);

        final NodeIdentifierWithPredicates tableKey = RibSupportUtils.toYangTablesKey(key);
        final InstanceIdentifierBuilder tableId = YangInstanceIdentifier.builder(this.yangRibId.node(LocRib.QNAME).node(Tables.QNAME));
        tableId.nodeWithKey(tableKey.getNodeType(), tableKey.getKeyValues());
        for (final Entry<QName, Object> e : tableKey.getKeyValues().entrySet()) {
            table.withChild(ImmutableNodes.leafNode(e.getKey(), e.getValue()));
        }

        final RIBSupportContext supportContext = this.ribContextRegistry.getRIBSupportContext(key);
        if (supportContext != null) {
            final ChoiceNode routes = supportContext.getRibSupport().emptyRoutes();
            table.withChild(routes);

            tx.put(LogicalDatastoreType.OPERATIONAL, tableId.build(), table.build());
            try {
                tx.submit().checkedGet();
            } catch (final TransactionCommitFailedException e1) {
                LOG.error("Failed to initiate LocRIB for key {}", key, e1);
            }
            createLocRibWriter(key);
        } else {
            LOG.warn("There's no registered RIB Context for {}", key.getAfi());
        }
    }

    private synchronized void createLocRibWriter(final TablesKey key) {
        LOG.debug("Creating LocRIB writer for key {}", key);
        final DOMTransactionChain txChain = createPeerChain(this);
        PathSelectionMode pathSelectionStrategy = this.bestPathSelectionStrategies.get(key);
        if (pathSelectionStrategy == null) {
            pathSelectionStrategy = BasePathSelectionModeFactory.createBestPathSelectionStrategy();
        }

        final LocRibWriter locRibWriter = LocRibWriter.create(this.ribContextRegistry, key, txChain,
                getYangRibId(), this.localAs, getService(), this.exportPolicyPeerTrackerMap.get(key), pathSelectionStrategy);
        registerTotalPathCounter(key, locRibWriter);
        registerTotalPrefixesCounter(key, locRibWriter);
        this.txChainToLocRibWriter.put(txChain, locRibWriter);
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper;
    }

    @Override
    public synchronized void close() throws Exception {
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

    @Nonnull
    @Override
    public Set<? extends BgpTableType> getLocalTables() {
        return this.localTables;
    }

    @Override
    public BGPDispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public synchronized void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Broken chain in RIB {} transaction {}", getInstanceIdentifier(), transaction != null ? transaction.getIdentifier() : null, cause);
        if (this.txChainToLocRibWriter.containsKey(chain)) {
            final LocRibWriter locRibWriter = this.txChainToLocRibWriter.remove(chain);
            final DOMTransactionChain newChain = createPeerChain(this);
            locRibWriter.restart(newChain);
            this.txChainToLocRibWriter.put(newChain, locRibWriter);
        }
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("RIB {} closed successfully", getInstanceIdentifier());
    }

    @Override
    public Set<TablesKey> getLocalTablesKeys() {
        return this.localTablesKeys;
    }

    @Override
    public DOMDataTreeChangeService getService() {
        return (DOMDataTreeChangeService) this.service;
    }

    @Override
    public YangInstanceIdentifier getYangRibId() {
        return this.yangRibId;
    }

    @Override
    public DOMTransactionChain createPeerChain(final TransactionChainListener listener) {
        return this.domDataBroker.createTransactionChain(listener);
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

    @Override
    public ImportPolicyPeerTracker getImportPolicyPeerTracker() {
        return this.importPolicyPeerTracker;
    }

    @Override
    public ExportPolicyPeerTracker getExportPolicyPeerTracker(final TablesKey tablesKey) {
        return this.exportPolicyPeerTrackerMap.get(tablesKey);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        this.isServiceInstantiated = true;
        setActive(true);
        this.domChain = this.domDataBroker.createTransactionChain(this);
        LOG.info("RIB Singleton Service {} instantiated, RIB {}", getIdentifier().getValue(), this.ribId.getValue());
        LOG.debug("Instantiating RIB table {} at {}", this.ribId, this.yangRibId);

        final ContainerNode bgpRib = Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(BgpRib.QNAME))
                .addChild(ImmutableNodes.mapNodeBuilder(Rib.QNAME).build()).build();

        final MapEntryNode ribInstance = Builders.mapEntryBuilder().withNodeIdentifier(
                new NodeIdentifierWithPredicates(Rib.QNAME, RIB_ID_QNAME, this.ribId.getValue()))
                .addChild(ImmutableNodes.leafNode(RIB_ID_QNAME, this.ribId.getValue()))
                .addChild(ImmutableNodes.mapNodeBuilder(Peer.QNAME).build())
                .addChild(Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(LocRib.QNAME))
                        .addChild(ImmutableNodes.mapNodeBuilder(Tables.QNAME).build())
                        .build()).build();


        final DOMDataWriteTransaction trans = this.domChain.newWriteOnlyTransaction();

        // merge empty BgpRib + Rib, to make sure the top-level parent structure is present
        trans.merge(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.builder().node(BgpRib.QNAME).build(), bgpRib);
        trans.put(LogicalDatastoreType.OPERATIONAL, this.yangRibId, ribInstance);

        try {
            trans.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            LOG.error("Failed to initiate RIB {}", this.yangRibId, e);
        }

        LOG.debug("Effective RIB created.");

        this.localTablesKeys.forEach(this::startLocRib);
    }

    @Override
    public synchronized ListenableFuture<Void> closeServiceInstance() {
        if (!this.isServiceInstantiated) {
            LOG.trace("RIB Singleton Service {} already closed, RIB {}", getIdentifier().getValue(),
                    this.ribId.getValue());
            return Futures.immediateFuture(null);
        }
        LOG.info("Close RIB Singleton Service {}, RIB {}", getIdentifier().getValue(), this.ribId.getValue());
        this.isServiceInstantiated = false;
        setActive(false);

        this.txChainToLocRibWriter.values().forEach(LocRibWriter::close);
        this.txChainToLocRibWriter.clear();

        final DOMDataWriteTransaction t = this.domChain.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, getYangRibId());
        final ListenableFuture<Void> cleanFuture = t.submit();

        this.domChain.close();
        return cleanFuture;
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.serviceGroupIdentifier;
    }

    @Override
    public ClusterSingletonServiceRegistration registerClusterSingletonService(
            final ClusterSingletonService clusterSingletonService) {
        return ClusterSingletonServiceRegistrationHelper
                .registerSingletonService(this.provider, clusterSingletonService);
    }

    @Override
    public ServiceGroupIdentifier getRibIServiceGroupIdentifier() {
        return getIdentifier();
    }
}
