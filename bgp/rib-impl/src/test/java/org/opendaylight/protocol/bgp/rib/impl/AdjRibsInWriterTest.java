/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertNotNull;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
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
import org.opendaylight.protocol.bgp.rib.spi.RibSupportUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.SupportedTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Attributes;
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

    private static final TablesKey k4 = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final Set<TablesKey> tableTypes = Sets.newHashSet(k4);
    private final String peerIp = "12.34.56.78";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.writer = AdjRibInWriter.create(YangInstanceIdentifier.of(Rib.QNAME), PeerRole.Ebgp, this.chain);
        assertNotNull(this.writer);
        Mockito.doReturn("MockedTrans").when(this.tx).toString();
        Mockito.doReturn(this.tx).when(this.chain).newWriteOnlyTransaction();
        Mockito.doReturn(Mockito.mock(CheckedFuture.class)).when(this.tx).submit();
    }

    @Test
    public void testTransform() {
        final YangInstanceIdentifier peerPath = YangInstanceIdentifier.builder().node(Rib.QNAME).node(Peer.QNAME).nodeWithKey(Peer.QNAME, AdjRibInWriter.PEER_ID_QNAME, this.peerIp).build();
        Mockito.doNothing().when(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doNothing().when(this.tx).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));
        Mockito.doReturn(this.context).when(this.registry).getRIBSupportContext(Mockito.any(TablesKey.class));
        Mockito.doNothing().when(this.context).clearTable(Mockito.eq(this.tx), Mockito.any(YangInstanceIdentifier.class));

        this.writer.transform(new PeerId(this.peerIp), this.registry, this.tableTypes, false);

        // verify peer skeleton was inserted correctly
        Mockito.verify(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath), Mockito.eq(this.writer.peerSkeleton(IdentifierUtils.peerKey(peerPath), this.peerIp, false)));
        // verify supported tables were inserted for ipv4
        Mockito.verify(this.tx).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(peerPath.node(SupportedTables.QNAME).node(RibSupportUtils.toYangKey(SupportedTables.QNAME, k4))), Mockito.any(NormalizedNode.class));
        // verify uptodate set to false
        final YangInstanceIdentifier path = peerPath.node(AdjRibIn.QNAME).node(Tables.QNAME).node(RibSupportUtils.toYangTablesKey(k4)).node(Attributes.QNAME).node(AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE.getNodeType());
        Mockito.verify(this.tx).merge(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Mockito.eq(path), Mockito.eq(AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE));
    }
}