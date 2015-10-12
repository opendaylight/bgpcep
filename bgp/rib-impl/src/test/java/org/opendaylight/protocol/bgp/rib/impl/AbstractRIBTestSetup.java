/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.Collection;
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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTreeFactory;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
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

    private RIBImpl rib;
    private final Ipv4Address clusterId = new Ipv4Address("128.0.0.1");
    private final Ipv4Address ribId = new Ipv4Address("127.0.0.1");
    static final Class<? extends AddressFamily> AFI = Ipv4AddressFamily.class;
    static final Class<? extends SubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;
    static final TablesKey KEY = new TablesKey(AFI, SAFI);
    private BindingCodecTreeFactory codecFactory;
    private RIBActivator a1;
    RIBSupport ribSupport;

    @Mock
    private BGPDispatcher dispatcher;

    @Mock
    private ReconnectStrategyFactory tcpStrategyFactory;

    @Mock
    private DataBroker dps;

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
    private CheckedFuture<?,?> future;

    @Mock
    private Optional<Rib> o;

    @Mock
    private DOMDataTreeChangeService service;

    @Before
    public void setUp() throws Exception {
        mockRib();
    }

    public void mockRib() throws Exception {
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        final ModuleInfoBackedContext strategy = createClassLoadingStrategy();
        final SchemaContext schemaContext = strategy.tryToCreateSchemaContext().get();
        this.codecFactory = createCodecFactory(strategy,schemaContext);
        final List<BgpTableType> localTables = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(AFI, SAFI));
        this.a1 = new RIBActivator();
        this.a1.startRIBExtensionProvider(context);
        mockedMethods();
        this.rib = new RIBImpl(new RibId("test"), new AsNumber(5L), this.ribId,
            this.clusterId, context , this.dispatcher, this.tcpStrategyFactory, this.codecFactory, this.tcpStrategyFactory, this.dps, this.dom, localTables, GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        this.rib.onGlobalContextUpdated(schemaContext);
        this.ribSupport = getRib().getRibSupportContext().getRIBSupportContext(KEY).getRibSupport();
    }

    private static ModuleInfoBackedContext createClassLoadingStrategy() {
        final ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        try {
            ctx.registerModuleInfo(BindingReflections.getModuleInfo(Ipv4Route.class));
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
        return ctx;
    }

    private static BindingCodecTreeFactory createCodecFactory(final ClassLoadingStrategy str, final SchemaContext ctx) {
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry codec = new BindingNormalizedNodeCodecRegistry(generator);
        codec.onBindingRuntimeContextUpdated(BindingRuntimeContext.create(str, ctx));
        return codec;
    }

    @SuppressWarnings("unchecked")
    private void mockedMethods() throws Exception {
        MockitoAnnotations.initMocks(this);
        final ReadOnlyTransaction readTx = Mockito.mock(ReadOnlyTransaction.class);
        Mockito.doReturn(null).when(this.service).registerDataTreeChangeListener(Mockito.any(DOMDataTreeIdentifier.class), Mockito.any(DOMDataTreeChangeListener.class));
        final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> map = new HashMap<>();
        map.put(DOMDataTreeChangeService.class, this.service);
        Mockito.doNothing().when(readTx).close();
        Mockito.doReturn(readTx).when(this.dps).newReadOnlyTransaction();
        final CheckedFuture<Optional<DataObject>, ReadFailedException> readFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doNothing().when(this.domTransWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doNothing().when(this.domTransWrite).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class));
        Mockito.doNothing().when(this.domTransWrite).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doReturn(Optional.<DataObject>absent()).when(readFuture).checkedGet();
        Mockito.doReturn(readFuture).when(readTx).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class));
        Mockito.doReturn(this.chain).when(this.dps).createTransactionChain(Mockito.any(RIBImpl.class));
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
            final NodeIdentifierWithPredicates routekey = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, IPv4RIBSupport.PREFIX_QNAME, p);
            final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
            b.withNodeIdentifier(routekey);
            b.addChild(Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(IPv4RIBSupport.PREFIX_QNAME)).withValue(p).build());

            final DataTreeCandidateNode child = Mockito.mock(DataTreeCandidateNode.class);
            Mockito.doReturn(createIdentifier(target, p)).when(child).getIdentifier();
            Mockito.doReturn(Optional.of(b.build())).when(child).getDataAfter();
            Mockito.doReturn(type).when(child).getModificationType();
            children.add(child);
        }
        Mockito.doReturn(children).when(rootNode).getChildNodes();
        col.add(candidate);
        return col;
    }

    public PathArgument createIdentifier(final YangInstanceIdentifier base, final Ipv4Prefix prefix) {
        final NodeIdentifierWithPredicates routekey = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, IPv4RIBSupport.PREFIX_QNAME, prefix);
        return YangInstanceIdentifier.of(IPv4RIBSupport.PREFIX_QNAME).node(routekey).getLastPathArgument();
    }

    public RIBImpl getRib() {
        return this.rib;
    }

    public DOMDataBroker getDOMBroker() {
        return this.dom;
    }

    public DOMDataWriteTransaction getTransaction() {
        return this.domTransWrite;
    }

    @After
    public void tearDown() {
        this.a1.close();
    }
}
