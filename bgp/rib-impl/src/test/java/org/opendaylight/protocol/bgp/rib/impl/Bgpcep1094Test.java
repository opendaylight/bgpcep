/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.parser.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

/**
 * Verifies that peers register before their table-creation events can trigger the initial route advertisement.
 */
public class Bgpcep1094Test extends AbstractRIBTestSetup {
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final String PREFIX = "1.1.1.0/24";
    private static final PeerId PEER_A = new PeerId("bgp://127.0.0.2");
    private static final PeerId PEER_B = new PeerId("bgp://127.0.0.3");
    private static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(AS));

    private final BGPPeerTracker peerTracker = new BGPPeerTrackerImpl();

    @Mock
    private Peer peerB;
    @Mock
    private DOMDataTreeWriteTransaction tx;
    @Mock
    private DOMTransactionChain chain;
    @Mock
    private Registration reg;
    @Mock
    private DataTreeChangeExtension dataBroker;
    @Mock
    private BGPSession session;
    private RIBExtensionProviderContext ribContext;
    private YangInstanceIdentifier ribIId;
    private MapEntryNode tableWithRoute;

    @Before
    public void setUpWriter() {
        ribContext = new SimpleRIBExtensionProviderContext();
        new RIBActivator().startRIBExtensionProvider(ribContext, mappingService.currentSerializer());
        ribIId = getRib().getYangRibId();
        tableWithRoute = buildTableWithRoute();

        doReturn(PEER_B).when(peerB).getPeerId();
        doReturn(PeerRole.RrClient).when(peerB).getRole();
        doReturn(true).when(peerB).supportsTable(any(TablesKey.class));
        doNothing().when(peerB).initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());

        doNothing().when(tx).put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(NormalizedNode.class));
        doNothing().when(tx).merge(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(NormalizedNode.class));
        doNothing().when(tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();

        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doNothing().when(chain).close();

        doNothing().when(reg).close();
        doReturn(reg).when(dataBroker).registerTreeChangeListener(any(DOMDataTreeIdentifier.class),
            any(DOMDataTreeChangeListener.class));
    }

    /**
     * A registered peer receives existing routes when its effective-rib-in table is created.
     */
    @Test
    public void testNewPeerReceivesExistingRoutes() {
        // walkThrough() must be able to consult the tracker before the first peer registers.
        assertTrue(peerTracker.getPeers().isEmpty());
        assertTrue(peerTracker.getNonInternalPeers().isEmpty());

        final var ribSupport = ribContext.getRIBSupport(TABLES_KEY);
        try (var locRibWriter = LocRibWriter.create(ribSupport, IPV4UNICAST.VALUE, chain, ribIId,
                AS_NUMBER, dataBroker, policies, peerTracker,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy())) {
            // PeerA already advertised a route, so the loc-rib holds a best path.
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_A, tableWithRoute)));
            // Registration precedes the table-creation event, even if initialization commits are still pending.
            peerTracker.registerPeer(peerB);
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_B, ribSupport.emptyTable())));

            // PeerB must still receive the route that existed when it connected. Capture the advertised routes and
            // assert the list is non-empty - an empty list would still match anyList() while exhibiting the defect.
            final var routes = ArgumentCaptor.forClass(List.class);
            verify(peerB).initializeRibOut(any(RouteEntryDependenciesContainer.class), routes.capture());
            assertFalse("new peer must receive the routes that existed when it connected",
                routes.getValue().isEmpty());
        }
    }

    /**
     * Registration must be synchronous and precede the commit which can trigger the table-creation event.
     */
    @Test
    public void testRegistrationPrecedesInitializationCommits() throws Exception {
        doNothing().when(session).close();
        doReturn(new Ipv4Address("127.0.0.3")).when(session).getBgpId();
        doReturn(Set.of(new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE)))
            .when(session).getAdvertisedTableTypes();
        doReturn(List.of()).when(session).getAdvertisedAddPathTableTypes();
        doReturn(GracefulRestartUtil.EMPTY_GR_CAPABILITY).when(session).getAdvertisedGracefulRestartCapability();
        doReturn(GracefulRestartUtil.EMPTY_LLGR_CAPABILITY).when(session).getAdvertisedLlGracefulRestartCapability();
        final var bgpPeer = AbstractAddPathTest.configurePeer(tableRegistry, new Ipv4AddressNoZone("127.0.0.3"),
            getRib(), null, PeerRole.Ibgp, new StrictBGPPeerRegistry());
        final var initialization = SettableFuture.<CommitInfo>create();
        doAnswer(invocation -> {
            assertTrue("initialization must hold the peer lock", Thread.holdsLock(bgpPeer));
            assertSame("peer must register before table creation is committed", bgpPeer,
                getRib().getPeerTracker().getPeer(PEER_B));
            return FluentFuture.from(initialization);
        }).when(getTransaction()).commit();

        try {
            bgpPeer.onSessionUp(session);
            assertFalse(initialization.isDone());
            assertSame("peer must be registered when onSessionUp returns", bgpPeer,
                getRib().getPeerTracker().getPeer(PEER_B));
        } finally {
            // Cleanup commits do not have the initialization ordering requirement.
            doReturn(CommitInfo.emptyFluentFuture()).when(getTransaction()).commit();
            initialization.set(CommitInfo.empty());
            bgpPeer.close();
        }
    }

    @Test
    public void testApplicationPeerRegistrationPrecedesInitializationCommit() {
        final var appPeer = new ApplicationPeer(tableRegistry, new ApplicationRibId("app-peer"),
            new Ipv4AddressNoZone("127.0.0.3"), getRib());
        final var initialization = SettableFuture.<CommitInfo>create();
        doAnswer(invocation -> {
            assertTrue("initialization must hold the peer lock", Thread.holdsLock(appPeer));
            assertSame("application peer must register before table creation is committed", appPeer,
                getRib().getPeerTracker().getPeer(PEER_B));
            return FluentFuture.from(initialization);
        }).when(getTransaction()).commit();

        try {
            appPeer.instantiateServiceInstance(dataBroker,
                DOMDataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, ribIId));
            assertFalse(initialization.isDone());
            assertSame(appPeer, getRib().getPeerTracker().getPeer(PEER_B));
        } finally {
            doReturn(CommitInfo.emptyFluentFuture()).when(getTransaction()).commit();
            initialization.set(CommitInfo.empty());
            appPeer.close();
        }
    }

    private DataTreeCandidate effRibInEvent(final PeerId peerId, final MapEntryNode tableNode) {
        final var tablePath = ribIId.node(PEER_NID)
            .node(IdentifierUtils.domPeerId(peerId))
            .node(EFFRIBIN_NID)
            .node(TABLES_NID)
            .node(tableNode.name());
        return DataTreeCandidates.fromNormalizedNode(tablePath, tableNode);
    }

    private MapEntryNode buildTableWithRoute() {
        final var attributes = new AttributesBuilder()
            .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.ZERO).build())
            .setAsPath(new AsPathBuilder().setSegments(List.of()).build())
            .build();
        final var route = new Ipv4RouteBuilder()
            .setRouteKey(PREFIX)
            .setPathId(new PathId(Uint32.ONE))
            .setPrefix(new Ipv4Prefix(PREFIX))
            .setAttributes(attributes)
            .build();
        final var routes = new Ipv4RoutesCaseBuilder()
            .setIpv4Routes(new Ipv4RoutesBuilder().setIpv4Route(Map.of(route.key(), route)).build())
            .build();
        final var table = new TablesBuilder().withKey(TABLES_KEY).setRoutes(routes).build();

        final var tableId = DataObjectIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("rib")))
            .child(LocRib.class)
            .child(Tables.class, TABLES_KEY)
            .build();
        final var serializer = (BindingNormalizedNodeSerializer) mappingService.currentSerializer();
        return (MapEntryNode) ((NodeResult) serializer.toNormalizedNode(tableId, table)).node();
    }
}
