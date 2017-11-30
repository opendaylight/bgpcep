/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
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

public class AbstractRIBTestSetup {

    protected static final Class<? extends AddressFamily> AFI = Ipv4AddressFamily.class;
    protected static final Class<? extends SubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;
    protected static final TablesKey KEY = new TablesKey(AFI, SAFI);
    protected static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final ClusterIdentifier CLUSTER_ID = new ClusterIdentifier("128.0.0.1");
    private static final BgpId RIB_ID = new BgpId("127.0.0.1");
    private RIBImpl rib;
    private BindingCodecTreeFactory codecFactory;
    private RIBActivator a1;
    private RIBSupport ribSupport;
    @Mock
    private BGPDispatcher dispatcher;

    @Mock
    private DOMDataBroker dom;

    @Mock
    private BindingTransactionChain chain;

    @Mock
    private WriteTransaction transWrite;

    @Mock
    private DOMTransactionChain domChain;

    @Mock
    private DOMDataWriteTransaction domTransWrite;

    @Mock
    private CheckedFuture<?, ?> future;

    @Mock
    private Optional<Rib> o;

    @Mock
    private DOMDataTreeChangeService service;

    @Mock
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    private static ModuleInfoBackedContext createClassLoadingStrategy() {
        final ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        try {
            ctx.registerModuleInfo(BindingReflections.getModuleInfo(Ipv4Route.class));
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return ctx;
    }

    private static BindingCodecTreeFactory createCodecFactory(final ClassLoadingStrategy str, final SchemaContext ctx) {
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry codec = new BindingNormalizedNodeCodecRegistry(generator);
        codec.onBindingRuntimeContextUpdated(BindingRuntimeContext.create(str, ctx));
        return codec;
    }

    @Before
    public void setUp() throws Exception {
        mockRib();
    }

    public void mockRib() throws Exception {
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        final ModuleInfoBackedContext strategy = createClassLoadingStrategy();
        final SchemaContext schemaContext = strategy.tryToCreateSchemaContext().get();
        this.codecFactory = createCodecFactory(strategy, schemaContext);
        final List<BgpTableType> localTables = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(AFI, SAFI));

        this.a1 = new RIBActivator();
        this.a1.startRIBExtensionProvider(context);
        mockedMethods();
        doReturn(Mockito.mock(ClusterSingletonServiceRegistration.class)).when(this.clusterSingletonServiceProvider)
                .registerClusterSingletonService(any(ClusterSingletonService.class));
        this.rib = new RIBImpl(this.clusterSingletonServiceProvider, new RibId("test"),
                new AsNumber(5L), RIB_ID, CLUSTER_ID, context,
                this.dispatcher, this.codecFactory, this.dom, localTables,
                Collections.singletonMap(new TablesKey(AFI, SAFI),
                        BasePathSelectionModeFactory.createBestPathSelectionStrategy()),
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        this.rib.onGlobalContextUpdated(schemaContext);
        this.ribSupport = getRib().getRibSupportContext().getRIBSupportContext(KEY).getRibSupport();
    }

    @SuppressWarnings("unchecked")
    private void mockedMethods() throws Exception {
        MockitoAnnotations.initMocks(this);
        final ReadOnlyTransaction readTx = Mockito.mock(ReadOnlyTransaction.class);
        Mockito.doReturn(new listenerRegistration()).when(this.service).registerDataTreeChangeListener(Mockito.any(DOMDataTreeIdentifier.class), Mockito.any(ClusteredDOMDataTreeChangeListener.class));
        final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> map = new HashMap<>();
        map.put(DOMDataTreeChangeService.class, this.service);
        Mockito.doNothing().when(readTx).close();
        final CheckedFuture<Optional<DataObject>, ReadFailedException> readFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doNothing().when(this.domTransWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doNothing().when(this.domTransWrite).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class));
        Mockito.doNothing().when(this.domTransWrite).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doReturn(Optional.absent()).when(readFuture).checkedGet();
        Mockito.doReturn(readFuture).when(readTx).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class));
        Mockito.doNothing().when(this.domChain).close();
        Mockito.doReturn(this.domTransWrite).when(this.domChain).newWriteOnlyTransaction();
        Mockito.doNothing().when(getTransaction()).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(YangInstanceIdentifier.of(BgpRib.QNAME)), Mockito.any(NormalizedNode.class));
        Mockito.doReturn(map).when(this.dom).getSupportedExtensions();
        Mockito.doReturn(this.domChain).when(this.dom).createTransactionChain(Mockito.any(BGPPeer.class));
        Mockito.doReturn(this.transWrite).when(this.chain).newWriteOnlyTransaction();
        Mockito.doReturn(false).when(this.o).isPresent();
        Mockito.doReturn(this.o).when(this.future).checkedGet();
        Mockito.doReturn(this.future).when(this.domTransWrite).submit();
        Mockito.doNothing().when(this.future).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doNothing().when(this.transWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class), Mockito.eq(true));
        Mockito.doNothing().when(this.transWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doReturn(this.future).when(this.transWrite).submit();
    }

    public Collection<DataTreeCandidate> ipv4Input(final YangInstanceIdentifier target, final ModificationType type, final Ipv4Prefix... prefix) {
        final Collection<DataTreeCandidate> col = new HashSet<>();
        final DataTreeCandidate candidate = Mockito.mock(DataTreeCandidate.class);
        final DataTreeCandidateNode rootNode = Mockito.mock(DataTreeCandidateNode.class);
        Mockito.doReturn(rootNode).when(candidate).getRootNode();
        Mockito.doReturn(type).when(rootNode).getModificationType();
        Mockito.doCallRealMethod().when(rootNode).toString();
        Mockito.doReturn(target).when(candidate).getRootPath();
        Mockito.doCallRealMethod().when(candidate).toString();
        final Collection<DataTreeCandidateNode> children = new HashSet<>();
        for (final Ipv4Prefix p : prefix) {
            final NodeIdentifierWithPredicates routekey = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, PREFIX_QNAME, p);
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routekey);
            b.addChild(Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(PREFIX_QNAME)).withValue(p).build());

            final DataTreeCandidateNode child = Mockito.mock(DataTreeCandidateNode.class);
            Mockito.doReturn(createIdentifier(p)).when(child).getIdentifier();
            Mockito.doReturn(Optional.of(b.build())).when(child).getDataAfter();
            Mockito.doReturn(type).when(child).getModificationType();
            children.add(child);
        }
        Mockito.doReturn(children).when(rootNode).getChildNodes();
        col.add(candidate);
        return col;
    }

    public PathArgument createIdentifier(final Ipv4Prefix prefix) {
        final NodeIdentifierWithPredicates routekey = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, PREFIX_QNAME, prefix);
        return YangInstanceIdentifier.of(PREFIX_QNAME).node(routekey).getLastPathArgument();
    }

    public RIBImpl getRib() {
        return this.rib;
    }

    public DOMDataWriteTransaction getTransaction() {
        return this.domTransWrite;
    }

    @After
    public void tearDown() {
        this.a1.close();
    }

    private class listenerRegistration implements ListenerRegistration<EventListener> {
        @Override
        public EventListener getInstance() {
            return null;
        }

        @Override
        public void close() {
        }
    }
}
