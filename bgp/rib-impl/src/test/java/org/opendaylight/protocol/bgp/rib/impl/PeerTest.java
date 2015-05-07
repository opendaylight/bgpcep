/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoop;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
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
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.spi.DefaultDataTreeCandidate;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class PeerTest {

    private final TablesKey tk = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private RIBImpl r;

    @Mock
    BGPDispatcher dispatcher;

    @Mock
    ReconnectStrategyFactory tcpStrategyFactory;

    @Mock
    DataBroker dps;

    @Mock
    DOMDataBroker dom;

    @Mock
    WriteTransaction transWrite;

    @Mock
    DOMDataWriteTransaction domTransWrite;

    @Mock
    BindingTransactionChain chain;

    @Mock
    DOMTransactionChain domChain;

    BindingCodecTreeFactory codecFactory;

    ApplicationPeer peer;

    @Mock
    CheckedFuture<?,?> future;

    @Mock
    Optional<Rib> o;

    @Mock
    DOMDataTreeChangeService service;

    BGPSessionImpl session;

    List<YangInstanceIdentifier> routes;

    private BGPPeer classic;

    @Mock
    Channel channel;

    @Mock
    ChannelPipeline pipeline;

    @Mock
    private EventLoop eventLoop;

    private RIBActivator a1;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final ModuleInfoBackedContext strategy = createClassLoadingStrategy();
        final SchemaContext schemaContext = strategy.tryToCreateSchemaContext().get();
        this.codecFactory = createCodecFactory(strategy,schemaContext);
        final List<BgpTableType> localTables = new ArrayList<>();
        this.routes = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        this.a1 = new RIBActivator();
        this.a1.startRIBExtensionProvider(context);
        Mockito.doReturn(this.chain).when(this.dps).createTransactionChain(Mockito.any(RIBImpl.class));
        Mockito.doReturn(this.domChain).when(this.dom).createTransactionChain(Mockito.any(BGPPeer.class));
        final Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> map = new HashMap<>();
        map.put(DOMDataTreeChangeService.class, this.service);
        Mockito.doReturn(null).when(this.service).registerDataTreeChangeListener(Mockito.any(DOMDataTreeIdentifier.class), Mockito.any(DOMDataTreeChangeListener.class));
        Mockito.doReturn(map).when(this.dom).getSupportedExtensions();
        Mockito.doReturn(this.o).when(this.future).checkedGet();
        Mockito.doNothing().when(this.domChain).close();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final NormalizedNode<?,?> node = (NormalizedNode<?,?>)args[2];
                if (node.getNodeType().equals(Ipv4Route.QNAME) || node.getNodeType().equals(IPv4RIBSupport.PREFIX_QNAME)) {
                    PeerTest.this.routes.add((YangInstanceIdentifier) args[1]);
                }
                return args[1];
            }
        }).when(this.domTransWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doNothing().when(this.domTransWrite).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                PeerTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(this.domTransWrite).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class));
        Mockito.doReturn(false).when(this.o).isPresent();
        Mockito.doReturn(this.future).when(this.domTransWrite).submit();
        Mockito.doNothing().when(this.future).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doReturn(this.transWrite).when(this.chain).newWriteOnlyTransaction();
        Mockito.doNothing().when(this.transWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class), Mockito.eq(true));
        Mockito.doNothing().when(this.transWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class), Mockito.any(DataObject.class));
        Mockito.doReturn(this.future).when(this.transWrite).submit();
        Mockito.doReturn(this.domTransWrite).when(this.domChain).newWriteOnlyTransaction();
        Mockito.doReturn(this.eventLoop).when(this.channel).eventLoop();
        Mockito.doReturn("channel").when(this.channel).toString();
        Mockito.doReturn(this.pipeline).when(this.channel).pipeline();
        Mockito.doReturn(this.pipeline).when(this.pipeline).addLast(Mockito.any(ChannelHandler.class));
        this.r = new RIBImpl(new RibId("test"), new AsNumber(5L), new Ipv4Address("127.0.0.1"),
            new Ipv4Address("128.0.0.1"), context , this.dispatcher, this.tcpStrategyFactory, this.codecFactory, this.tcpStrategyFactory, this.dps, this.dom, localTables,GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
        this.peer = new ApplicationPeer(new ApplicationRibId("t"), new Ipv4Address("127.0.0.1"), this.r);
        this.r.onGlobalContextUpdated(schemaContext);
        final ReadOnlyTransaction readTx = Mockito.mock(ReadOnlyTransaction.class);
        Mockito.doNothing().when(readTx).close();
        Mockito.doReturn(readTx).when(this.dps).newReadOnlyTransaction();
        final CheckedFuture<Optional<DataObject>, ReadFailedException> readFuture = Mockito.mock(CheckedFuture.class);
        Mockito.doReturn(Optional.<DataObject>absent()).when(readFuture).checkedGet();
        Mockito.doReturn(readFuture).when(readTx).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(InstanceIdentifier.class));
    }

    private BindingCodecTreeFactory createCodecFactory(final ClassLoadingStrategy str, final SchemaContext ctx) {
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry codec = new BindingNormalizedNodeCodecRegistry(generator);
        codec.onBindingRuntimeContextUpdated(BindingRuntimeContext.create(str, ctx));
        return codec;
    }

    private ModuleInfoBackedContext createClassLoadingStrategy() {
        final ModuleInfoBackedContext ctx = ModuleInfoBackedContext.create();
        try {
            ctx.registerModuleInfo(BindingReflections.getModuleInfo(Ipv4Route.class));
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
        return ctx;
    }

    @After
    public void tearDown() {
        this.a1.close();
    }

    @Test
    public void testAppPeer() {
        final Collection<DataTreeCandidate> changes = new ArrayList<>();
        final RIBSupport support = this.r.getRibSupportContext().getRIBSupportContext(this.tk).getRibSupport();

        final YangInstanceIdentifier base = this.r.getYangRibId().node(LocRib.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(this.tk));

        final NodeIdentifierWithPredicates routekey = new NodeIdentifierWithPredicates(Ipv4Route.QNAME, IPv4RIBSupport.PREFIX_QNAME, new Ipv4Prefix("127.0.0.1/32"));
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> b = ImmutableNodes.mapEntryBuilder();
        b.withNodeIdentifier(routekey);
        b.addChild(Builders.leafBuilder().withNodeIdentifier(new NodeIdentifier(IPv4RIBSupport.PREFIX_QNAME)).withValue("127.0.0.1/32").build());

        changes.add(new DefaultDataTreeCandidate(support.routePath(base.node(Routes.QNAME), routekey), DataTreeCandidateNodes.fromNormalizedNode(b.build())));

        this.peer.onDataTreeChanged(changes);
        assertEquals(1, this.routes.size());
    }

    @Test
    public void testClassicPeer() {
        this.classic = new BGPPeer("testPeer", this.r);
        Mockito.doReturn(null).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        Mockito.doReturn(Boolean.TRUE).when(this.channel).isWritable();
        Mockito.doReturn(null).when(this.channel).close();
        Mockito.doReturn(new DefaultChannelPromise(this.channel)).when(this.channel).writeAndFlush(any(Notification.class));

        Mockito.doReturn(new InetSocketAddress("localhost", 12345)).when(this.channel).remoteAddress();
        Mockito.doReturn(new InetSocketAddress("localhost", 12345)).when(this.channel).localAddress();
        final List<BgpParameters> params = Lists.newArrayList(new BgpParametersBuilder().setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder().setCParameters(new MultiprotocolCaseBuilder()
            .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build())).build());
        this.session = new BGPSessionImpl(this.classic, this.channel, new OpenBuilder().setBgpIdentifier(new Ipv4Address("1.1.1.1")).setHoldTimer(50).setMyAsNumber(72).setBgpParameters(params).build(), 30, null);
        assertEquals("testPeer", this.classic.getName());
        this.classic.onSessionUp(this.session);
        Assert.assertArrayEquals(new byte[] {1, 1, 1, 1}, this.classic.getRawIdentifier());
        assertEquals("BGPPeer{name=testPeer, tables=[TablesKey [_afi=class org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily, _safi=class org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily]]}", this.classic.toString());
        final List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("127.0.0.1/32"), new Ipv4Prefix("2.2.2.2/24"));
        final UpdateBuilder ub = new UpdateBuilder();
        ub.setNlri(new NlriBuilder().setNlri(prefs).build());
        ub.setAttributes(new AttributesBuilder().build());
        this.classic.onMessage(this.session, ub.build());
        assertEquals(2, this.routes.size());

        //create new peer so that it gets advertized routes from RIB
        try (final BGPPeer testingPeer = new BGPPeer("testingPeer", this.r)) {
            testingPeer.onSessionUp(this.session);
            assertEquals(2, this.routes.size());
            assertEquals(1, testingPeer.getBgpPeerState().getSessionEstablishedCount().intValue());
            assertEquals(1, testingPeer.getBgpPeerState().getRouteTable().size());
            assertNotNull(testingPeer.getBgpSessionState());
        }

        ub.setNlri(null);
        ub.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(prefs).build());
        this.classic.onMessage(this.session, ub.build());
        assertEquals(0, this.routes.size());
        this.classic.onMessage(this.session, new KeepaliveBuilder().build());
        this.classic.onMessage(this.session, new UpdateBuilder().setAttributes(
            new AttributesBuilder().addAugmentation(
                    Attributes2.class,
                    new Attributes2Builder().setMpUnreachNlri(
                            new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).build()).build()).build()).build());
        this.classic.releaseConnection();
    }

    @After
    public void cleanUp() {
        this.peer.close();
    }
}
