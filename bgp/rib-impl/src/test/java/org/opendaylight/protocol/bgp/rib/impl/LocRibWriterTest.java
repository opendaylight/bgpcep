/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.LOCRIB_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.ROUTES_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.tree.api.ModificationType;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

@ExtendWith(MockitoExtension.class)
class LocRibWriterTest {
    private static final PeerId PEER_A = new PeerId("bgp://127.0.0.2");
    private static final PeerId PEER_B = new PeerId("bgp://127.0.0.3");
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final QName ROUTE_QNAME = QName.create("urn:opendaylight:test:route", "route");
    private static final QName ROUTE_KEY_QNAME = QName.create(ROUTE_QNAME, "route-key");
    private static final QName LABEL_QNAME = QName.create(ROUTE_QNAME, "label");
    private static final NodeIdentifier ROUTE_NID = NodeIdentifier.create(ROUTE_QNAME);
    private static final NodeIdentifierWithPredicates ROUTE_ID =
        NodeIdentifierWithPredicates.of(ROUTE_QNAME, ROUTE_KEY_QNAME, "test-route");
    private static final NodeIdentifierWithPredicates TABLE_ID = NodeIdentifierWithPredicates.of(
        TABLES_NID.getNodeType(), QName.create(TABLES_NID.getNodeType(), "afi"), "ipv4");
    private static final YangInstanceIdentifier RIB_ID = YangInstanceIdentifier.of(BgpRib.QNAME);
    private static final YangInstanceIdentifier LOC_ROUTE_ID = RIB_ID.node(LOCRIB_NID).node(TABLES_NID).node(TABLE_ID)
        .node(ROUTES_NID).node(ROUTE_NID).node(ROUTE_ID);
    private static final ContainerNode ATTRIBUTES = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(NodeIdentifier.create(Attributes.QNAME))
        .withChild(ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(NodeIdentifier.create(Origin.QNAME))
            .withChild(ImmutableNodes.leafNode(QName.create(Origin.QNAME, "value"), "igp"))
            .build())
        .build();

    private final BGPPeerTrackerImpl peerTracker = new BGPPeerTrackerImpl();
    @Mock
    private RIBSupport<Ipv4RoutesCase, Ipv4Routes> ribSupport;
    @Mock
    private DOMTransactionChain chain;
    @Mock
    private DOMDataTreeWriteTransaction tx;
    @Mock
    private DataTreeChangeExtension dataBroker;
    @Mock
    private Registration registration;
    @Mock
    private BGPRibRoutingPolicy policies;
    @Mock
    private Peer receivingPeer;
    @Captor
    private ArgumentCaptor<List<AdvertizedRoute>> advertisedRoutes;

    @BeforeEach
    void setUp() {
        doReturn(table()).when(ribSupport).emptyTable();
        doReturn(TABLES_KEY).when(ribSupport).getTablesKey();
        doAnswer(inv -> ((DataTreeCandidateNode) inv.getArgument(0)).modifiedChild(ROUTE_NID).childNodes())
            .when(ribSupport).changedRoutes(any(DataTreeCandidateNode.class));
        doReturn("test-route").when(ribSupport).extractRouteKey(ROUTE_ID);
        doReturn(Uint32.ZERO).when(ribSupport).extractPathId(ROUTE_ID);
        doReturn(ATTRIBUTES).when(ribSupport).extractAttributes(any(MapEntryNode.class));
        // Preserve the route content: these tests exercise selection and propagation, not NLRI conversion.
        doAnswer(inv -> inv.getArgument(0)).when(ribSupport).createRoute(any(MapEntryNode.class),
            any(NodeIdentifierWithPredicates.class), any(ContainerNode.class));
        doReturn(ROUTE_ID).when(ribSupport).toAddPathListArgument(ROUTE_ID);
        doReturn(ROUTE_ID).when(ribSupport).toNonPathListArgument(ROUTE_ID);
        doReturn(LOC_ROUTE_ID).when(ribSupport).createRouteIdentifier(any(YangInstanceIdentifier.class), eq(ROUTE_ID));

        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doNothing().when(chain).close();
        doNothing().when(tx).put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(NormalizedNode.class));
        doNothing().when(tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();
        doReturn(registration).when(dataBroker).registerTreeChangeListener(any(DOMDataTreeIdentifier.class),
            any(DOMDataTreeChangeListener.class));
        doNothing().when(registration).close();

        doReturn(new PeerId("bgp://127.0.0.4")).when(receivingPeer).getPeerId();
        doReturn(PeerRole.Ebgp).when(receivingPeer).getRole();
        doReturn(true).when(receivingPeer).supportsTable(TABLES_KEY);
        doNothing().when(receivingPeer).refreshRibOut(any(RouteEntryDependenciesContainer.class), anyList(), anyList());
        peerTracker.registerPeer(receivingPeer);
    }

    @ParameterizedTest
    @MethodSource("pathSelectionModes")
    void testWriteReplacementAdvertisesChangedLabel(final PathSelectionMode mode) {
        final var before = route(1000);
        final var after = route(2000);
        try (var writer = createWriter(mode)) {
            writer.onDataTreeChanged(List.of(event(PEER_A, table(), table(before))));
            assertCounts(writer, 1, 1);
            assertAdvertised(before);
            clearInvocations(tx, receivingPeer);

            final var replacement = event(PEER_A, table(before), table(after));
            final var routeChange = replacement.getRootNode().modifiedChild(ROUTES_NID)
                .modifiedChild(ROUTE_NID).modifiedChild(ROUTE_ID);
            assertEquals(ModificationType.WRITE, routeChange.modificationType());
            assertEquals(before, routeChange.dataBefore());
            writer.onDataTreeChanged(List.of(replacement));

            assertCounts(writer, 1, 1);
            assertAdvertised(after);

            writer.onDataTreeChanged(List.of(event(PEER_A, table(after), table())));
            assertCounts(writer, 0, 0);
            verify(tx).delete(LogicalDatastoreType.OPERATIONAL, LOC_ROUTE_ID);
        }
    }

    @ParameterizedTest
    @MethodSource("pathSelectionModes")
    void testIdenticalWriteKeepsPathCount(final PathSelectionMode mode) {
        final var route = route(1000);
        try (var writer = createWriter(mode)) {
            writer.onDataTreeChanged(List.of(event(PEER_A, table(), table(route))));
            clearInvocations(tx, receivingPeer);

            // A delivered WRITE conservatively refreshes the selected route, even if its data are equal.
            writer.onDataTreeChanged(List.of(event(PEER_A, table(route), table(route))));
            assertCounts(writer, 1, 1);
            assertAdvertised(route);

            writer.onDataTreeChanged(List.of(event(PEER_A, table(route), table())));
            assertCounts(writer, 0, 0);
            verify(tx).delete(LogicalDatastoreType.OPERATIONAL, LOC_ROUTE_ID);
        }
    }

    @ParameterizedTest
    @MethodSource("pathSelectionModes")
    void testNewPeerPathIncreasesOnlyPathCount(final PathSelectionMode mode) {
        final var route = route(1000);
        try (var writer = createWriter(mode)) {
            writer.onDataTreeChanged(List.of(event(PEER_A, table(), table(route))));
            assertCounts(writer, 1, 1);

            writer.onDataTreeChanged(List.of(event(PEER_B, table(), table(route))));
            assertCounts(writer, 2, 1);

            writer.onDataTreeChanged(List.of(event(PEER_B, table(route), table())));
            assertCounts(writer, 1, 1);

            writer.onDataTreeChanged(List.of(event(PEER_A, table(route), table())));
            assertCounts(writer, 0, 0);
        }
    }

    private LocRibWriter createWriter(final PathSelectionMode mode) {
        doReturn(ROUTE_ID).when(ribSupport).createRouteListArgument(anyString());
        if (mode instanceof AllPathSelection) {
            doReturn(ROUTE_ID).when(ribSupport).createRouteListArgument(any(Uint32.class), anyString());
        }
        return LocRibWriter.create(ribSupport, IPV4UNICAST.VALUE, chain, RIB_ID,
            new AsNumber(Uint32.valueOf(64496)), dataBroker, policies, peerTracker, mode);
    }

    private void assertAdvertised(final MapEntryNode expected) {
        verify(tx).put(LogicalDatastoreType.OPERATIONAL, LOC_ROUTE_ID, expected);
        verify(receivingPeer).refreshRibOut(any(RouteEntryDependenciesContainer.class), eq(List.of()),
            advertisedRoutes.capture());
        final var routes = advertisedRoutes.getValue();
        assertEquals(1, routes.size());
        assertEquals(expected, routes.getFirst().getRoute());
        assertEquals(ATTRIBUTES, routes.getFirst().getAttributes());
        assertEquals(PEER_A, routes.getFirst().getFromPeerId());
    }

    private static void assertCounts(final LocRibWriter writer, final long paths, final long prefixes) {
        assertEquals(paths, writer.getPathsCount());
        assertEquals(prefixes, writer.getPrefixesCount());
    }

    private static Stream<PathSelectionMode> pathSelectionModes() {
        return Stream.of(BasePathSelectionModeFactory.createBestPathSelectionStrategy(), new AllPathSelection());
    }

    private static DataTreeCandidate event(final PeerId peer, final MapEntryNode before, final MapEntryNode after) {
        final var beforeTables = ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(TABLES_NID)
            .withChild(before).build();
        final var afterTables = ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(TABLES_NID)
            .withChild(after).build();
        return DataTreeCandidates.newDataTreeCandidate(RIB_ID.node(PEER_NID).node(IdentifierUtils.domPeerId(peer))
            .node(EFFRIBIN_NID).node(TABLES_NID).node(TABLE_ID),
            DataTreeCandidateNodes.containerDelta(beforeTables, afterTables, TABLE_ID));
    }

    private static MapEntryNode table(final MapEntryNode... routes) {
        return ImmutableNodes.newMapEntryBuilder().withNodeIdentifier(TABLE_ID)
            .withChild(ImmutableNodes.newContainerBuilder().withNodeIdentifier(ROUTES_NID)
                .withChild(ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(ROUTE_NID)
                    .withValue(List.of(routes)).build())
                .build())
            .build();
    }

    private static MapEntryNode route(final int label) {
        return ImmutableNodes.newMapEntryBuilder().withNodeIdentifier(ROUTE_ID)
            .withChild(ImmutableNodes.leafNode(LABEL_QNAME, Uint32.valueOf(label))).build();
    }
}
