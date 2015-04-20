/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public final class RIBImpl extends DefaultRibReference implements AutoCloseable, RIB, TransactionChainListener, SchemaContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
    private static final QName RIB_ID_QNAME = QName.cachedReference(QName.create(Rib.QNAME, "id"));
    private static final ContainerNode EMPTY_TABLE_ATTRIBUTES = ImmutableNodes.containerNode(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes.QNAME);

    private final ReconnectStrategyFactory tcpStrategyFactory;
    private final ReconnectStrategyFactory sessionStrategyFactory;

    private final BGPDispatcher dispatcher;
    private final DOMTransactionChain domChain;
    private final AsNumber localAs;
    private final Ipv4Address bgpIdentifier;
    private final ClusterIdentifier clusterId;
    private final Set<BgpTableType> localTables;
    private final Set<TablesKey> localTablesKeys;
    private final DataBroker dataBroker;
    private final DOMDataBroker domDataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final YangInstanceIdentifier yangRibId;
    private final RIBSupportContextRegistryImpl ribContextRegistry;
    private final EffectiveRibInWriter efWriter;
    private final DOMDataBrokerExtension service;

    public RIBImpl(final RibId ribId, final AsNumber localAs, final Ipv4Address localBgpId, final Ipv4Address clusterId, final RIBExtensionConsumerContext extensions,
        final BGPDispatcher dispatcher, final ReconnectStrategyFactory tcpStrategyFactory, final BindingCodecTreeFactory codecFactory,
        final ReconnectStrategyFactory sessionStrategyFactory, final DataBroker dps, final DOMDataBroker domDataBroker, final List<BgpTableType> localTables, final GeneratedClassLoadingStrategy classStrategy) {
        super(InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(Preconditions.checkNotNull(ribId))));
        this.domChain = domDataBroker.createTransactionChain(this);
        this.localAs = Preconditions.checkNotNull(localAs);
        this.bgpIdentifier = Preconditions.checkNotNull(localBgpId);
        this.clusterId = (clusterId == null) ? new ClusterIdentifier(localBgpId) : new ClusterIdentifier(clusterId);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.sessionStrategyFactory = Preconditions.checkNotNull(sessionStrategyFactory);
        this.tcpStrategyFactory = Preconditions.checkNotNull(tcpStrategyFactory);
        this.localTables = ImmutableSet.copyOf(localTables);
        this.localTablesKeys = new HashSet<TablesKey>();
        this.dataBroker = dps;
        this.domDataBroker = Preconditions.checkNotNull(domDataBroker);
        this.extensions = Preconditions.checkNotNull(extensions);
        this.ribContextRegistry = RIBSupportContextRegistryImpl.create(extensions, codecFactory, classStrategy);
        this.yangRibId = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Rib.QNAME).nodeWithKey(Rib.QNAME, RIB_ID_QNAME, ribId.getValue()).build();

        LOG.debug("Instantiating RIB table {} at {}", ribId, this.yangRibId);

        final ContainerNode rib = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(BgpRib.QNAME))
            .addChild(Builders.mapBuilder().withNodeIdentifier(new NodeIdentifier(Rib.QNAME))
                .addChild(Builders.mapEntryBuilder().withNodeIdentifier(new NodeIdentifierWithPredicates(Rib.QNAME, RIB_ID_QNAME, ribId.getValue()))
                    .addChild(ImmutableNodes.leafNode(RIB_ID_QNAME, ribId.getValue()))
                    .addChild(ImmutableNodes.mapNodeBuilder(Peer.QNAME).build())
                    .addChild(Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(LocRib.QNAME)).build())
                .build())
            .build())
            .build();


        final DOMDataWriteTransaction trans = this.domChain.newWriteOnlyTransaction();

        // put empty BgpRib if not exists
        trans.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.builder().node(BgpRib.QNAME).build(), rib);

        Futures.addCallback(trans.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Change committed successfully");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initiate RIB {}", getInstanceIdentifier(), t);
            }

        });

        final PolicyDatabase pd  = new PolicyDatabase(localAs.getValue(), localBgpId, this.clusterId);

        final DOMDataBrokerExtension service = this.domDataBroker.getSupportedExtensions().get(DOMDataTreeChangeService.class);
        this.service = service;
        this.efWriter = EffectiveRibInWriter.create(getService(), this.createPeerChain(this), getYangRibId(), pd, this.ribContextRegistry);
        LOG.debug("Effective RIB created.");

        for (final BgpTableType t : localTables) {
            final TablesKey key = new TablesKey(t.getAfi(), t.getSafi());
            this.localTablesKeys.add(key);
            // create locRibWriter for each table
            final DOMDataWriteTransaction tx = this.domChain.newWriteOnlyTransaction();

            final NormalizedNodeContainerBuilder<NodeIdentifier, PathArgument, MapEntryNode, MapNode> table = ImmutableNodes.mapNodeBuilder(Tables.QNAME)
                .addChild(Builders.mapEntryBuilder().withNodeIdentifier(RibSupportUtils.toYangTablesKey(key))
                    .addChild(EMPTY_TABLE_ATTRIBUTES).build());
            tx.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.builder(this.yangRibId.node(LocRib.QNAME).node(Tables.QNAME)).build(), table.build());
            Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.trace("Change committed successfully");
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to initiate LocRIB {}", getInstanceIdentifier(), t);
                }

            });
            LocRibWriter.create(this.ribContextRegistry, key, this.createPeerChain(this), getYangRibId(), localAs, getService(), pd);
        }
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper;
    }

    @Override
    public synchronized void close() throws InterruptedException, ExecutionException {
        final DOMDataWriteTransaction t = this.domChain.newWriteOnlyTransaction();
        t.delete(LogicalDatastoreType.OPERATIONAL, getYangRibId());
        t.submit().get();
        this.domChain.close();
        this.efWriter.close();
    }

    @Override
    public AsNumber getLocalAs() {
        return this.localAs;
    }

    @Override
    public Ipv4Address getBgpIdentifier() {
        return this.bgpIdentifier;
    }

    @Override
    public Set<? extends BgpTableType> getLocalTables() {
        return this.localTables;
    }

    @Override
    public ReconnectStrategyFactory getTcpStrategyFactory() {
        return this.tcpStrategyFactory;
    }

    @Override
    public ReconnectStrategyFactory getSessionStrategyFactory() {
        return this.sessionStrategyFactory;
    }

    @Override
    public BGPDispatcher getDispatcher() {
        return this.dispatcher;
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction, final Throwable cause) {
        LOG.error("Broken chain in RIB {} transaction {}", getInstanceIdentifier(), transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.info("RIB {} closed successfully", getInstanceIdentifier());
    }

    @Override
    public long getRoutesCount(final TablesKey key) {
        try {
            final Optional<Tables> tableMaybe = this.dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL,
                    getInstanceIdentifier().child(LocRib.class).child(Tables.class, key)).checkedGet();
            if (tableMaybe.isPresent()) {
                final Tables table = tableMaybe.get();
                if (table.getRoutes() instanceof Ipv4RoutesCase) {
                    final Ipv4RoutesCase routesCase = (Ipv4RoutesCase) table.getRoutes();
                    if (routesCase.getIpv4Routes() != null && routesCase.getIpv4Routes().getIpv4Route() != null) {
                        return routesCase.getIpv4Routes().getIpv4Route().size();
                    }
                } else if (table.getRoutes() instanceof Ipv6RoutesCase) {
                    final Ipv6RoutesCase routesCase = (Ipv6RoutesCase) table.getRoutes();
                    if (routesCase.getIpv6Routes() != null && routesCase.getIpv6Routes().getIpv6Route() != null) {
                        return routesCase.getIpv6Routes().getIpv6Route().size();
                    }
                }
            }
        } catch (final ReadFailedException e) {
            //no-op
        }
        return 0;
    }

    public Set<TablesKey> getLocalTablesKeys() {
        return this.localTablesKeys;
    }

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
        this.ribContextRegistry.onSchemaContextUpdated(context);
    }
}
