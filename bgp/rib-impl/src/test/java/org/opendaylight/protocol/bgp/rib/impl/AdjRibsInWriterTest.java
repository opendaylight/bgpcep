/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.SimpleRoutingPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class AdjRibsInWriterTest {

    @Mock
    private DOMTransactionChain chain;

    @Mock
    private DOMDataWriteTransaction tx;

    @Mock
    private RIBSupportContextRegistry registry;

    @Mock
    private RIBSupportContext context;

    private AdjRibInWriter writer;

    private static final TablesKey K4 = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final Set<TablesKey> tableTypes = Sets.newHashSet(K4);
    private static final Map<TablesKey, SendReceive> ADD_PATH_TABLE_MAPS = Collections.singletonMap(K4, SendReceive.Both);

    private final String peerIp = "12.34.56.78";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn("MockedTrans").when(this.tx).toString();
        Mockito.doReturn(this.tx).when(this.chain).newWriteOnlyTransaction();
        final CheckedFuture<?, ?> checkedFuture =  Mockito.mock(CheckedFuture.class);
        Mockito.doNothing().when(checkedFuture).addListener(any(), any());
        Mockito.doReturn(checkedFuture).when(this.tx).submit();
        Mockito.doNothing().when(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doNothing().when(this.tx).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doReturn(this.context).when(this.registry).getRIBSupportContext(Mockito.any(TablesKey.class));
        Mockito.doNothing().when(this.context).createEmptyTableStructure(Mockito.eq(this.tx), Mockito.any(YangInstanceIdentifier.class));
    }

    @Test
    public void testTransform() {
        this.writer = AdjRibInWriter.create(YangInstanceIdentifier.of(Rib.QNAME), PeerRole.Ebgp, Optional.empty(), this.chain);
        assertNotNull(this.writer);
        final YangInstanceIdentifier peerPath = YangInstanceIdentifier.builder().node(Rib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME,
            AdjRibInWriter.PEER_ID_QNAME, this.peerIp).build();
        this.writer.transform(new PeerId(this.peerIp), this.registry, this.tableTypes, ADD_PATH_TABLE_MAPS);
        verifyPeerSkeletonInsertedCorrectly(peerPath);
        // verify supported tables were inserted for ipv4
        Mockito.verify(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath.node(SupportedTables.QNAME)
            .node(RibSupportUtils.toYangKey(SupportedTables.QNAME, K4))), Mockito.any(NormalizedNode.class));
        verifyUptodateSetToFalse(peerPath);
    }

    private void verifyUptodateSetToFalse(final YangInstanceIdentifier peerPath) {
        final YangInstanceIdentifier path = peerPath.node(AdjRibIn.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(K4))
            .node(Attributes.QNAME).node(AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE.getNodeType());
        Mockito.verify(this.tx).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(path), Mockito.eq(AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE));
    }

    private void verifyPeerSkeletonInsertedCorrectly(final YangInstanceIdentifier peerPath) {
        Mockito.verify(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath),
            Mockito.eq(this.writer.peerSkeleton(IdentifierUtils.peerKey(peerPath), this.peerIp)));
    }

    @Test
    public void testAnnounceNoneTransform() {
        this.writer = AdjRibInWriter.create(YangInstanceIdentifier.of(Rib.QNAME), PeerRole.Ebgp, Optional.of(SimpleRoutingPolicy.AnnounceNone), this.chain);
        assertNotNull(this.writer);
        final YangInstanceIdentifier peerPath = YangInstanceIdentifier.builder().node(Rib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME,
            AdjRibInWriter.PEER_ID_QNAME, this.peerIp).build();
        this.writer.transform(new PeerId(this.peerIp), this.registry, this.tableTypes, ADD_PATH_TABLE_MAPS);
        verifyPeerSkeletonInsertedCorrectly(peerPath);
        // verify supported tables were not inserted for ipv4, AnnounceNone
        Mockito.verify(this.tx, never()).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath.node(SupportedTables.QNAME)
            .node(RibSupportUtils.toYangKey(SupportedTables.QNAME, K4))), Mockito.any(NormalizedNode.class));
        verifyUptodateSetToFalse(peerPath);

    }
}