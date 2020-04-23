/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.binding.runtime.spi.GeneratedClassLoadingStrategy;
import org.opendaylight.binding.runtime.spi.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class AbstractRIBTestSetup extends DefaultRibPoliciesMockTest {

    static final Class<? extends AddressFamily> IPV4_AFI = Ipv4AddressFamily.class;
    private static final Class<? extends AddressFamily> IPV6_AFI = Ipv6AddressFamily.class;
    static final Class<? extends SubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;
    static final TablesKey KEY = new TablesKey(IPV4_AFI, SAFI);
    static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final BgpId RIB_ID = new BgpId("127.0.0.1");
    private RIBImpl rib;
    private BindingCodecTreeFactory codecFactory;
    private RIBActivator a1;
    private RIBSupport<?, ?, ?, ?> ribSupport;
    @Mock
    private BGPDispatcher dispatcher;

    @Mock
    private DOMDataBroker dom;

    @Mock
    private TransactionChain chain;

    @Mock
    private WriteTransaction transWrite;

    @Mock
    private DOMTransactionChain domChain;

    @Mock
    private DOMDataTreeWriteTransaction domTransWrite;

    @Mock
    private FluentFuture<? extends CommitInfo> future;

    @Mock
    private DOMDataTreeChangeService service;

    @Mock
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    private static ModuleInfoBackedContext createClassLoadingStrategy() throws Exception {
        final ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        ctx.registerModuleInfo(BindingReflections.getModuleInfo(Ipv4Route.class));
        return ctx;
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockRib();
    }

    public void mockRib() throws Exception {
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        final ModuleInfoBackedContext strategy = createClassLoadingStrategy();
        final SchemaContext schemaContext = strategy.getEffectiveModelContext();
        this.codecFactory = new BindingNormalizedNodeCodecRegistry(
            BindingRuntimeContext.create(strategy, schemaContext));
        final List<BgpTableType> localTables = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(IPV4_AFI, SAFI));
        localTables.add(new BgpTableTypeImpl(IPV6_AFI, SAFI));

        this.a1 = new RIBActivator();
        this.a1.startRIBExtensionProvider(context, this.mappingService);

        final CodecsRegistryImpl codecsRegistry = CodecsRegistryImpl.create(this.codecFactory,
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());

        mockedMethods();
        doReturn(mock(ClusterSingletonServiceRegistration.class)).when(this.clusterSingletonServiceProvider)
                .registerClusterSingletonService(any(ClusterSingletonService.class));
        this.rib = new RIBImpl(this.tableRegistry, new RibId("test"), new AsNumber(Uint32.valueOf(5)), RIB_ID, context,
                this.dispatcher, codecsRegistry, this.dom, getDataBroker(), this.policies,
                localTables, Collections.singletonMap(KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy()));
        this.rib.onGlobalContextUpdated(schemaContext);
        this.ribSupport = getRib().getRibSupportContext().getRIBSupport(KEY);
    }

    @SuppressWarnings("unchecked")
    private void mockedMethods() throws Exception {
        MockitoAnnotations.initMocks(this);
        final ReadTransaction readTx = mock(ReadTransaction.class);
        doReturn(new TestListenerRegistration()).when(this.service)
                .registerDataTreeChangeListener(any(DOMDataTreeIdentifier.class),
                        any(ClusteredDOMDataTreeChangeListener.class));
        doNothing().when(readTx).close();
        doNothing().when(this.domTransWrite).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(this.domTransWrite).delete(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class));
        doNothing().when(this.domTransWrite).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doReturn(FluentFutures.immediateFluentFuture(Optional.empty())).when(readTx)
            .read(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
        doNothing().when(this.domChain).close();
        doReturn(this.domTransWrite).when(this.domChain).newWriteOnlyTransaction();
        doNothing().when(getTransaction()).put(eq(LogicalDatastoreType.OPERATIONAL),
                eq(YangInstanceIdentifier.of(BgpRib.QNAME)), any(NormalizedNode.class));
        doReturn(ImmutableClassToInstanceMap.of(DOMDataTreeChangeService.class, this.service)).when(this.dom)
            .getExtensions();
        doReturn(this.domChain).when(this.dom).createMergingTransactionChain(any(AbstractPeer.class));
        doReturn(this.transWrite).when(this.chain).newWriteOnlyTransaction();
        doReturn(Optional.empty()).when(this.future).get();
        doReturn(this.future).when(this.domTransWrite).commit();
        doNothing().when(this.future).addListener(any(Runnable.class), any(Executor.class));
        doNothing().when(this.transWrite).mergeParentStructurePut(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class), any(DataObject.class));
        doNothing().when(this.transWrite).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(InstanceIdentifier.class), any(DataObject.class));
        doReturn(this.future).when(this.transWrite).commit();
    }

    public Collection<DataTreeCandidate> ipv4Input(final YangInstanceIdentifier target,
            final ModificationType type, final Ipv4Prefix... prefix) {
        final Collection<DataTreeCandidate> col = new HashSet<>();
        final DataTreeCandidate candidate = mock(DataTreeCandidate.class);
        final DataTreeCandidateNode rootNode = mock(DataTreeCandidateNode.class);
        doReturn(rootNode).when(candidate).getRootNode();
        doReturn(type).when(rootNode).getModificationType();
        doCallRealMethod().when(rootNode).toString();
        doReturn(target).when(candidate).getRootPath();
        doCallRealMethod().when(candidate).toString();
        final Collection<DataTreeCandidateNode> children = new HashSet<>();
        for (final Ipv4Prefix p : prefix) {
            final NodeIdentifierWithPredicates routekey =
                    NodeIdentifierWithPredicates.of(Ipv4Route.QNAME, PREFIX_QNAME, p);
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b =
                    ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routekey);
            b.addChild(Builders.leafBuilder()
                    .withNodeIdentifier(new NodeIdentifier(PREFIX_QNAME)).withValue(p).build());

            final DataTreeCandidateNode child = mock(DataTreeCandidateNode.class);
            doReturn(createIdentifier(p)).when(child).getIdentifier();
            doReturn(java.util.Optional.of(b.build())).when(child).getDataAfter();
            doReturn(type).when(child).getModificationType();
            children.add(child);
        }
        doReturn(children).when(rootNode).getChildNodes();
        col.add(candidate);
        return col;
    }

    public PathArgument createIdentifier(final Ipv4Prefix prefix) {
        final NodeIdentifierWithPredicates routekey =
                NodeIdentifierWithPredicates.of(Ipv4Route.QNAME, PREFIX_QNAME, prefix);
        return YangInstanceIdentifier.of(PREFIX_QNAME).node(routekey).getLastPathArgument();
    }

    public RIBImpl getRib() {
        return this.rib;
    }

    public DOMDataTreeWriteTransaction getTransaction() {
        return this.domTransWrite;
    }

    @Override
    @After
    public void tearDown() {
        this.a1.close();
    }

    private class TestListenerRegistration implements ListenerRegistration<EventListener> {
        @Override
        public EventListener getInstance() {
            return null;
        }

        @Override
        public void close() {
        }
    }
}
