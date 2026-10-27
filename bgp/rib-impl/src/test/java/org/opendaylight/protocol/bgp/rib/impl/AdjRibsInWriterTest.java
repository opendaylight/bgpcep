/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContext;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.RIBNormalizedNodes;
import org.opendaylight.protocol.bgp.rib.spi.RIBQNames;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

@ExtendWith(MockitoExtension.class)
class AdjRibsInWriterTest {
    private static final TablesKey K4 = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    private static final NodeIdentifierWithPredicates DOM_K4 = NodeIdentifierWithPredicates.of(Tables.QNAME, Map.of(
        RIBQNames.AFI_QNAME, Ipv4AddressFamily.QNAME, RIBQNames.SAFI_QNAME, UnicastSubsequentAddressFamily.QNAME));
    private static final Map<TablesKey, SendReceive> ADD_PATH_TABLE_MAPS = Map.of(K4, SendReceive.Both);
    private static final Set<TablesKey> TABLE_TYPES = Set.of(K4);
    private static final String PEER_IP = "12.34.56.78";

    @Mock
    private DOMDataTreeWriteTransaction tx;
    @Mock
    private RIBSupportContext context;
    @Mock
    private AbstractRIBSupport<?, ?, ?> support;

    @Test
    void testTransform() {
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();
        doNothing().when(tx).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(tx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(context).createEmptyTableStructure(eq(tx), any(YangInstanceIdentifier.class));
        doReturn(support).when(context).ribSupport();
        doReturn(DOM_K4).when(support).tablesKey();

        final var peerPath = YangInstanceIdentifier.builder()
            .node(RIB_NID)
            .node(Peer.QNAME).nodeWithKey(Peer.QNAME, RIBQNames.PEER_ID_QNAME, PEER_IP)
            .build();
        final var writer = new AdjRibInWriter(PeerRole.Ebgp, AdjRibInTables.of(peerPath, TABLE_TYPES, type -> context));
        assertThat(writer.initialize(tx, new PeerId(PEER_IP), peerPath, ADD_PATH_TABLE_MAPS)).isDone();

        verify(tx).put(eq(LogicalDatastoreType.OPERATIONAL), eq(peerPath),
            eq(writer.peerSkeleton(IdentifierUtils.peerKey(peerPath), PEER_IP)));
        // verify supported tables were inserted for ipv4
        verify(tx).put(eq(LogicalDatastoreType.OPERATIONAL), eq(peerPath.node(SupportedTables.QNAME)
            .node(NodeIdentifierWithPredicates.of(SupportedTables.QNAME, DOM_K4.asMap()))), any(NormalizedNode.class));
        verify(tx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(peerPath.node(ADJRIBIN_NID)
            .node(TABLES_NID).node(DOM_K4)
            .node(ATTRIBUTES_NID).node(UPTODATE_NID)), eq(RIBNormalizedNodes.ATTRIBUTES_UPTODATE_FALSE));
    }
}
