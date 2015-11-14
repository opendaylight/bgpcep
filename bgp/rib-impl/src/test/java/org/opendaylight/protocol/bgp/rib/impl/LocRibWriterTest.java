/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class LocRibWriterTest {

    @Mock
    DOMDataWriteTransaction domTransWrite;

    private final PolicyDatabase pd = new PolicyDatabase((long) 35, new Ipv4Address("10.25.2.9"), new ClusterIdentifier(new Ipv4Address("10.25.2.9")));

    @Mock
    private RIBSupportContextRegistry registry;

    @Mock
    private DOMTransactionChain chain;

    @Mock
    private DOMDataTreeChangeService service;

    @Mock
    private RIBSupportContext context;

    @Mock
    private AbstractIPRIBSupport ribSupport;

    @Mock
    CheckedFuture<?, ?> future;

    private LocRibWriter locRibWriter;

    private final List<YangInstanceIdentifier> routes = new ArrayList<>();
    private final TablesKey tablesKey = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final NormalizedNode<?, ?> node = (NormalizedNode<?, ?>) args[2];
                if (node.getNodeType().equals(Ipv4Route.QNAME) || node.getNodeType().equals(IPv4RIBSupport.PREFIX_QNAME)) {
                    LocRibWriterTest.this.routes.add((YangInstanceIdentifier) args[1]);
                }
                return args[1];
            }
        }).when(this.domTransWrite).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final NormalizedNode<?, ?> node = (NormalizedNode<?, ?>) args[2];
                if (node.getNodeType().equals(Ipv4Route.QNAME) || node.getNodeType().equals(IPv4RIBSupport.PREFIX_QNAME)) {
                    LocRibWriterTest.this.routes.add((YangInstanceIdentifier) args[1]);
                }
                return args[1];
            }
        }).when(this.domTransWrite).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                LocRibWriterTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(this.domTransWrite).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class));
        Mockito.doReturn(this.context).when(this.registry).getRIBSupportContext(this.tablesKey);
        Mockito.doReturn(this.ribSupport).when(this.context).getRibSupport();
        Mockito.doReturn(this.domTransWrite).when(this.chain).newWriteOnlyTransaction();
        Mockito.doReturn(this.future).when(this.domTransWrite).submit();
        Mockito.doReturn(null).when(this.service).registerDataTreeChangeListener(Mockito.any(DOMDataTreeIdentifier.class), Mockito.any(DOMDataTreeChangeListener.class));
        Mockito.doReturn(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(Routes.QNAME)).addChild(Builders.containerBuilder().withNodeIdentifier(new NodeIdentifier(Ipv4Routes.QNAME)).withChild(ImmutableNodes.mapNodeBuilder(Ipv4Route.QNAME).build()).build()).build()).when(this.ribSupport).emptyRoutes();
        this.locRibWriter = LocRibWriter.create(this.registry, this.tablesKey, this.chain, YangInstanceIdentifier.of(BgpRib.QNAME), new AsNumber((long) 35), this.service, this.pd);
    }

    private DataTreeCandidate prepareUpdate() {
        final DataTreeCandidate candidate = Mockito.mock(DataTreeCandidate.class);
        Mockito.doReturn("candidate").when(candidate).toString();
        final YangInstanceIdentifier rootPath = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME, AdjRibInWriter.PEER_ID_QNAME, "12.12.12.12").build();
        Mockito.doReturn(rootPath).when(candidate).getRootPath();
        return candidate;
    }

    @Test
    public void testUpdateSupportedTables() {
        final DataTreeCandidate candidate = prepareUpdate();
        final DataTreeCandidateNode node = Mockito.mock(DataTreeCandidateNode.class);
        Mockito.doReturn("node").when(node).toString();
        final DataTreeCandidateNode tableChange = Mockito.mock(DataTreeCandidateNode.class);
        // add
        final DataTreeCandidateNode table = Mockito.mock(DataTreeCandidateNode.class);
        final NormalizedNode<?, ?> dataAfter = Mockito.mock(NormalizedNode.class);
        Mockito.doReturn(RibSupportUtils.toYangTablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)).when(dataAfter).getIdentifier();
        Mockito.doReturn(Optional.of(dataAfter)).when(table).getDataAfter();
        Mockito.doReturn(Lists.newArrayList(table)).when(tableChange).getChildNodes();
        Mockito.doReturn("table change").when(tableChange).toString();
        Mockito.doReturn(tableChange).when(node).getModifiedChild(YangInstanceIdentifier.of(SupportedTables.QNAME).getLastPathArgument());
        Mockito.doReturn(null).when(node).getModifiedChild(AbstractPeerRoleTracker.PEER_ROLE_NID);
        Mockito.doReturn(null).when(node).getModifiedChild(YangInstanceIdentifier.of(EffectiveRibIn.QNAME).getLastPathArgument());
        Mockito.doReturn(node).when(candidate).getRootNode();
        this.locRibWriter.onDataTreeChanged(Lists.newArrayList(candidate));
        // delete
        final DataTreeCandidateNode tableDelete = Mockito.mock(DataTreeCandidateNode.class);
        Mockito.doReturn(Optional.absent()).when(tableDelete).getDataAfter();
        Mockito.doReturn(Lists.newArrayList(tableDelete)).when(tableChange).getChildNodes();
        Mockito.doReturn("table change").when(tableChange).toString();
        Mockito.doReturn(node).when(candidate).getRootNode();
        this.locRibWriter.onDataTreeChanged(Lists.newArrayList(candidate));
        Mockito.verify(node, Mockito.times(2)).getModifiedChild(YangInstanceIdentifier.of(SupportedTables.QNAME).getLastPathArgument());
    }
}
