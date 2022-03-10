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
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.dom.adapter.CurrentAdapterSerializer;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;

public class AbstractRIBTestSetup extends DefaultRibPoliciesMockTest {

    static final Class<? extends AddressFamily> IPV4_AFI = Ipv4AddressFamily.class;
    private static final Class<? extends AddressFamily> IPV6_AFI = Ipv6AddressFamily.class;
    static final Class<? extends SubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;
    static final TablesKey KEY = new TablesKey(IPV4_AFI, SAFI);
    static final QName PREFIX_QNAME = QName.create(Ipv4Route.QNAME, "prefix").intern();
    private static final BgpId RIB_ID = new BgpId("127.0.0.1");
    private RIBImpl rib;
    private final RIBActivator a1 = new RIBActivator();
    @Mock
    private BGPDispatcher dispatcher;

    @Mock
    private DOMDataBroker dom;

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

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockRib();
    }

    public void mockRib() throws Exception {
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();
        final List<BgpTableType> localTables = new ArrayList<>();
        localTables.add(new BgpTableTypeImpl(IPV4_AFI, SAFI));
        localTables.add(new BgpTableTypeImpl(IPV6_AFI, SAFI));

        final CurrentAdapterSerializer serializer = mappingService.currentSerializer();
        a1.startRIBExtensionProvider(context, serializer);

        mockedMethods();
        doReturn(mock(ClusterSingletonServiceRegistration.class)).when(clusterSingletonServiceProvider)
                .registerClusterSingletonService(any(ClusterSingletonService.class));
        rib = new RIBImpl(tableRegistry, new RibId("test"), new AsNumber(Uint32.valueOf(5)), RIB_ID, context,
                dispatcher, new ConstantCodecsRegistry(serializer), dom, policies,
                localTables, Collections.singletonMap(KEY,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy()));
    }

    private void mockedMethods() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(new TestListenerRegistration()).when(service)
                .registerDataTreeChangeListener(any(DOMDataTreeIdentifier.class),
                        any(ClusteredDOMDataTreeChangeListener.class));
        doNothing().when(domTransWrite).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(domTransWrite).delete(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class));
        doNothing().when(domTransWrite).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(domChain).close();
        doReturn(domTransWrite).when(domChain).newWriteOnlyTransaction();
        doNothing().when(getTransaction()).put(eq(LogicalDatastoreType.OPERATIONAL),
                eq(YangInstanceIdentifier.of(BgpRib.QNAME)), any(NormalizedNode.class));
        doReturn(ImmutableClassToInstanceMap.of(DOMDataTreeChangeService.class, service)).when(dom)
            .getExtensions();
        doReturn(domChain).when(dom).createMergingTransactionChain(any(DOMTransactionChainListener.class));
        doReturn(Optional.empty()).when(future).get();
        doReturn(future).when(domTransWrite).commit();
        doNothing().when(future).addListener(any(Runnable.class), any(Executor.class));
    }

    public List<DataTreeCandidate> ipv4Input(final YangInstanceIdentifier target,
            final ModificationType type, final Ipv4Prefix... prefix) {
        final List<DataTreeCandidate> col = new ArrayList<>();
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
        return rib;
    }

    public DOMDataTreeWriteTransaction getTransaction() {
        return domTransWrite;
    }

    private static class TestListenerRegistration implements ListenerRegistration<EventListener> {
        @Override
        public EventListener getInstance() {
            return null;
        }

        @Override
        public void close() {
        }
    }
}
