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

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class AdjRibsInWriterTest {

    private static final TablesKey K4 = new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    private static final Map<TablesKey, SendReceive> ADD_PATH_TABLE_MAPS
            = Collections.singletonMap(K4, SendReceive.Both);
    private final Set<TablesKey> tableTypes = Sets.newHashSet(K4);
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
    private PeerTransactionChain ptc;
    private AdjRibInWriter writer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn("MockedTrans").when(tx).toString();
        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();
        doNothing().when(tx).put(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doNothing().when(tx).merge(eq(LogicalDatastoreType.OPERATIONAL),
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doReturn(context).when(registry).getRIBSupportContext(any(TablesKey.class));
        doReturn(chain).when(ptc).getDomChain();
        doNothing().when(context).createEmptyTableStructure(eq(tx), any(YangInstanceIdentifier.class));
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
                .node(RibSupportUtils.toYangKey(SupportedTables.QNAME, K4))), any(NormalizedNode.class));
        verifyUptodateSetToFalse(peerPath);
    }

    private void verifyUptodateSetToFalse(final YangInstanceIdentifier peerPath) {
        final YangInstanceIdentifier path = peerPath.node(ADJRIBIN_NID)
                .node(TABLES_NID).node(RibSupportUtils.toYangTablesKey(K4))
                .node(ATTRIBUTES_NID).node(UPTODATE_NID);
        verify(tx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(path),
                eq(RIBNormalizedNodes.ATTRIBUTES_UPTODATE_FALSE));
    }

    private void verifyPeerSkeletonInsertedCorrectly(final YangInstanceIdentifier peerPath) {
        verify(tx).put(eq(LogicalDatastoreType.OPERATIONAL), eq(peerPath),
                eq(writer.peerSkeleton(IdentifierUtils.peerKey(peerPath), peerIp)));
    }
}
