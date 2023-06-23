/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ADJRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ATTRIBUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.RIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.UPTODATE_NID;

import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.protocol.bgp.rib.spi.RIBQNames;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AdjRibsInWriterTest {
    private static final TablesKey K4 = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    private static final NodeIdentifierWithPredicates DOM_K4 = NodeIdentifierWithPredicates.of(Tables.QNAME, Map.of(
        RIBQNames.AFI_QNAME, Ipv4AddressFamily.QNAME, RIBQNames.SAFI_QNAME, UnicastSubsequentAddressFamily.QNAME));
    private static final Map<TablesKey, SendReceive> ADD_PATH_TABLE_MAPS = Map.of(K4, SendReceive.Both);

    private final Set<TablesKey> tableTypes = Set.of(K4);
    private final String peerIp = "12.34.56.78";

    @Mock
    private DOMTransactionChain chain;
    @Mock
    private DOMDataTreeWriteTransaction tx;
    @Mock
    private RIBSupportContextRegistry registry;
    @Mock
    private RIBSupportContext context;
    @Mock
    private RIBSupport<?, ?> support;
    @Mock
    private PeerTransactionChain ptc;

    private AdjRibInWriter writer;

    @Before
    public void setUp() {
        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();
        doNothing().when(tx).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(tx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doReturn(context).when(registry).getRIBSupportContext(any(TablesKey.class));
        doReturn(chain).when(ptc).getDomChain();
        doNothing().when(context).createEmptyTableStructure(eq(tx), any(YangInstanceIdentifier.class));
        doReturn(support).when(context).getRibSupport();
        doReturn(DOM_K4).when(support).tablesKey();
    }

    @Test
    public void testTransform() {
        writer = AdjRibInWriter.create(YangInstanceIdentifier.of(Rib.QNAME), PeerRole.Ebgp, ptc);
        assertNotNull(writer);
        final YangInstanceIdentifier peerPath = YangInstanceIdentifier.builder().node(RIB_NID)
                .node(Peer.QNAME).nodeWithKey(Peer.QNAME,
                        RIBQNames.PEER_ID_QNAME, peerIp).build();
        writer.transform(new PeerId(peerIp), peerPath, registry, tableTypes, ADD_PATH_TABLE_MAPS);
        verifyPeerSkeletonInsertedCorrectly(peerPath);
        // verify supported tables were inserted for ipv4
        verify(tx).put(eq(LogicalDatastoreType.OPERATIONAL), eq(peerPath.node(SupportedTables.QNAME)
            .node(NodeIdentifierWithPredicates.of(SupportedTables.QNAME, DOM_K4.asMap()))), any(NormalizedNode.class));
        verifyUptodateSetToFalse(peerPath);
    }

    private void verifyUptodateSetToFalse(final YangInstanceIdentifier peerPath) {
        final YangInstanceIdentifier path = peerPath.node(ADJRIBIN_NID)
                .node(TABLES_NID).node(DOM_K4)
                .node(ATTRIBUTES_NID).node(UPTODATE_NID);
        verify(tx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(path),
                eq(RIBNormalizedNodes.ATTRIBUTES_UPTODATE_FALSE));
    }

    private void verifyPeerSkeletonInsertedCorrectly(final YangInstanceIdentifier peerPath) {
        verify(tx).put(eq(LogicalDatastoreType.OPERATIONAL), eq(peerPath),
                eq(writer.peerSkeleton(IdentifierUtils.peerKey(peerPath), peerIp)));
    }
}
