/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.impl.AbstractRIBTestSetup.PREFIX_QNAME;
import static org.opendaylight.protocol.bgp.rib.spi.PeerRoleUtil.PEER_ROLE_NID;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

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
    CheckedFuture<?, ?> future;

    private LocRibWriter locRibWriter;

    private final List<YangInstanceIdentifier> routes = new ArrayList<>();
    private final TablesKey tablesKey = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Answer<Object> answer = new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final NormalizedNode<?, ?> node = (NormalizedNode<?, ?>) args[2];
                if (node.getNodeType().equals(Ipv4Route.QNAME) || node.getNodeType().equals(PREFIX_QNAME)) {
                    LocRibWriterTest.this.routes.add((YangInstanceIdentifier) args[1]);
                }
                return args[1];
            }
        };
        doAnswer(answer).when(this.domTransWrite).put(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doAnswer(answer).when(this.domTransWrite).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                LocRibWriterTest.this.routes.remove(args[1]);
                return args[1];
            }
        }).when(this.domTransWrite).delete(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class));
        doReturn(this.context).when(this.registry).getRIBSupportContext(any(TablesKey.class));
        doReturn(IPv4RIBSupport.getInstance()).when(this.context).getRibSupport();
        doReturn(this.domTransWrite).when(this.chain).newWriteOnlyTransaction();
        doReturn(this.future).when(this.domTransWrite).submit();
        doReturn(null).when(this.service).registerDataTreeChangeListener(any(DOMDataTreeIdentifier.class), any(DOMDataTreeChangeListener.class));
        this.locRibWriter = LocRibWriter.create(this.registry, this.tablesKey, this.chain, YangInstanceIdentifier.of(BgpRib.QNAME),
            new AsNumber((long) 35), this.service, this.pd, new CacheDisconnectedPeersImpl(), BasePathSelectionModeFactory.createBestPathSelectionStrategy());
    }

    private DataTreeCandidate prepareUpdate() {
        final DataTreeCandidate candidate = mock(DataTreeCandidate.class);
        doReturn("candidate").when(candidate).toString();
        final YangInstanceIdentifier rootPath = YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME, AdjRibInWriter.PEER_ID_QNAME, "12.12.12.12").build();
        doReturn(rootPath).when(candidate).getRootPath();
        return candidate;
    }

    @Test
    public void testUpdateSupportedTables() {
        final DataTreeCandidate candidate = prepareUpdate();
        final DataTreeCandidateNode node = mock(DataTreeCandidateNode.class);
        doReturn("node").when(node).toString();
        final DataTreeCandidateNode tableChange = mock(DataTreeCandidateNode.class);
        // add
        final DataTreeCandidateNode table = mock(DataTreeCandidateNode.class);
        final NormalizedNode<?, ?> dataAfter = mock(NormalizedNode.class);
        doReturn(RibSupportUtils.toYangTablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class)).when(dataAfter).getIdentifier();
        doReturn(Optional.of(dataAfter)).when(table).getDataAfter();
        doReturn(Lists.newArrayList(table)).when(tableChange).getChildNodes();
        doReturn("table change").when(tableChange).toString();
        doReturn(tableChange).when(node).getModifiedChild(YangInstanceIdentifier.of(SupportedTables.QNAME).getLastPathArgument());
        doReturn(null).when(node).getModifiedChild(PEER_ROLE_NID);
        doReturn(null).when(node).getModifiedChild(YangInstanceIdentifier.of(EffectiveRibIn.QNAME).getLastPathArgument());
        doReturn(node).when(candidate).getRootNode();
        doReturn(ModificationType.SUBTREE_MODIFIED).when(node).getModificationType();
        this.locRibWriter.onDataTreeChanged(Lists.newArrayList(candidate));
        // delete
        final DataTreeCandidateNode tableDelete = mock(DataTreeCandidateNode.class);
        doReturn(Optional.absent()).when(tableDelete).getDataAfter();
        doReturn(Lists.newArrayList(tableDelete)).when(tableChange).getChildNodes();
        doReturn("table change").when(tableChange).toString();
        doReturn(node).when(candidate).getRootNode();
        this.locRibWriter.onDataTreeChanged(Lists.newArrayList(candidate));
        verify(node, times(2)).getModifiedChild(YangInstanceIdentifier.of(SupportedTables.QNAME).getLastPathArgument());
    }
}
