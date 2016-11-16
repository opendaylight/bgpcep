/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.never;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.protocol.bgp.state.impl.neighbor.BGPNeighborStateImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class AdjRibsInWriterTest extends AbstractBgpStateHandler {

    @Mock
    private DOMTransactionChain chain;
    @Mock
    private DOMDataWriteTransaction tx;
    @Mock
    private RIBSupportContextRegistry registry;
    @Mock
    private RIBSupportContext context;

    private AdjRibInWriter writer;
    private final Set<TablesKey> tableTypes = Sets.newHashSet(TABLE_KEY);
    private static final Map<TablesKey, SendReceive> ADD_PATH_TABLE_MAPS = Collections.singletonMap(TABLE_KEY, SendReceive.Both);
    private final PeerId peerId = new PeerId(NEIGHBOR_ADDRESS.getIpv4Address().getValue());
    protected BGPNeighborStateImpl neighborState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn("MockedTrans").when(this.tx).toString();
        Mockito.doReturn(this.tx).when(this.chain).newWriteOnlyTransaction();
        Mockito.doReturn(Mockito.mock(CheckedFuture.class)).when(this.tx).submit();
        Mockito.doNothing().when(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doNothing().when(this.tx).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doReturn(this.context).when(this.registry).getRIBSupportContext(Mockito.any(TablesKey.class));
        Mockito.doNothing().when(this.context).createEmptyTableStructure(Mockito.eq(this.tx), Mockito.any(YangInstanceIdentifier.class));
        this.neighborState = new BGPNeighborStateImpl(NEIGHBOR_ADDRESS, AFI_SAFI, Collections.emptySet());
    }

    @Test
    public void testTransform() {
        this.writer = AdjRibInWriter.create(YangInstanceIdentifier.of(Rib.QNAME), PeerRole.Ebgp, Optional.empty(), this.chain, this.neighborState);
        assertNotNull(this.writer);
        final YangInstanceIdentifier peerPath = YangInstanceIdentifier.builder().node(Rib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME,
            AdjRibInWriter.PEER_ID_QNAME, NEIGHBOR_ADDRESS.getIpv4Address().getValue()).build();
        this.writer.transform(peerId, this.registry, this.tableTypes, ADD_PATH_TABLE_MAPS);
        verifyPeerSkeletonInsertedCorrectly(peerPath);
        // verify supported tables were inserted for ipv4
        Mockito.verify(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath.node(SupportedTables.QNAME)
            .node(RibSupportUtils.toYangKey(SupportedTables.QNAME, TABLE_KEY))), Mockito.any(NormalizedNode.class));
        verifyUptodateSetToFalse(peerPath);
    }

    private void verifyUptodateSetToFalse(final YangInstanceIdentifier peerPath) {
        final YangInstanceIdentifier path = peerPath.node(AdjRibIn.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(TABLE_KEY))
            .node(Attributes.QNAME).node(AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE.getNodeType());
        Mockito.verify(this.tx).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(path), Mockito.eq(AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE));
    }

    private void verifyPeerSkeletonInsertedCorrectly(final YangInstanceIdentifier peerPath) {
        Mockito.verify(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath),
            Mockito.eq(this.writer.peerSkeleton(IdentifierUtils.peerKey(peerPath), peerId.getValue())));
    }

    @Test
    public void testAnnounceNoneTransform() {
        this.writer = AdjRibInWriter.create(YangInstanceIdentifier.of(Rib.QNAME), PeerRole.Ebgp, Optional.of(SimpleRoutingPolicy.AnnounceNone), this.chain, this.neighborState);
        assertNotNull(this.writer);
        final YangInstanceIdentifier peerPath = YangInstanceIdentifier.builder().node(Rib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME,
            AdjRibInWriter.PEER_ID_QNAME, NEIGHBOR_ADDRESS.getIpv4Address().getValue()).build();
        this.writer.transform(peerId, this.registry, this.tableTypes, ADD_PATH_TABLE_MAPS);
        verifyPeerSkeletonInsertedCorrectly(peerPath);
        // verify supported tables were not inserted for ipv4, AnnounceNone
        Mockito.verify(this.tx, never()).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath.node(SupportedTables.QNAME)
            .node(RibSupportUtils.toYangKey(SupportedTables.QNAME, TABLE_KEY))), Mockito.any(NormalizedNode.class));
        verifyUptodateSetToFalse(peerPath);

    }
}