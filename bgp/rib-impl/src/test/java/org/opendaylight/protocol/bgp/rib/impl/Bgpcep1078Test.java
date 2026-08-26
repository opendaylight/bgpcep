/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.parser.GracefulRestartUtil;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RouterIds;
import org.opendaylight.protocol.bgp.rib.spi.entry.AdvertizedRoute;
import org.opendaylight.protocol.bgp.rib.spi.entry.RibOutEntryFactory;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRibRoutingPolicy;
import org.opendaylight.protocol.bgp.rib.spi.policy.BGPRouteEntryExportParameters;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Covers what a {@link RibOutEntryFactory#newSharing()} factory shares between peers and what it does not.
 *
 * <p>An entry depends on the route, the route key and the attributes, so peers whose export policy changed nothing
 * get one instance. A peer whose attributes were rewritten gets its own, and so does a peer using add-path, whose
 * key carries the path id.
 */
public class Bgpcep1078Test extends AbstractRIBTestSetup {
    private static final TablesKey IPV4_UNICAST = new TablesKey(Ipv4AddressFamily.VALUE,
        UnicastSubsequentAddressFamily.VALUE);
    private static final String PREFIX = "1.1.1.0/24";
    // Non-zero so the add-path key and the non-add-path one differ. RIBSupport.toNonPathListArgument() returns its
    // argument unchanged for path id zero, which would hide what addPathPeerGetsItsOwnEntry() checks.
    private static final Uint32 PATH_ID = Uint32.ONE;
    private static final Uint32 LOCAL_PREF = Uint32.valueOf(100);
    private static final Uint32 REWRITTEN_LOCAL_PREF = Uint32.valueOf(200);
    // The advertising peer. Distinct from every receiving peer, otherwise AbstractPeer.filterRoutes() drops the
    // route.
    private static final PeerId SOURCE_PEER = RouterIds.createPeerId(new Ipv4Address("9.9.9.9"));

    @Mock
    private RouteEntryDependenciesContainer entryDep;
    @Mock
    private BGPRibRoutingPolicy routingPolicies;
    @Mock
    private Peer sourcePeer;
    @Mock
    private AfiSafiType afiSafiType;

    private RIBSupport<Ipv4RoutesCase, Ipv4Routes> ribSupport;
    private AdvertizedRoute<Ipv4RoutesCase, Ipv4Routes> advRoute;
    private RibOutEntryFactory entryFactory;
    private BGPPeer peerA;
    private BGPPeer peerB;
    private DOMDataTreeWriteTransaction txA;
    private DOMDataTreeWriteTransaction txB;

    @Before
    public void setUpPeers() {
        // Only has to be resolvable in the peer tracker. The export policy stub ignores it.
        doReturn(SOURCE_PEER).when(sourcePeer).getPeerId();
        doReturn(PeerRole.Ebgp).when(sourcePeer).getRole();
        getRib().getPeerTracker().registerPeer(sourcePeer);

        peerA = bringUpPeer(new Ipv4AddressNoZone("127.0.0.2"), "2.2.2.2", false);
        peerB = bringUpPeer(new Ipv4AddressNoZone("127.0.0.3"), "3.3.3.3", false);
        txA = attachCapturingChain(peerA);
        txB = attachCapturingChain(peerB);

        // Type arguments written out so the AdvertizedRoute below takes them from here instead of from the
        // registry lookup.
        ribSupport = getRib().getRibSupportContext().getRIBSupport(IPV4_UNICAST);
        doReturn(ribSupport).when(entryDep).getRIBSupport();
        doReturn(getRib().getPeerTracker()).when(entryDep).getPeerTracker();
        doReturn(routingPolicies).when(entryDep).getRoutingPolicies();
        doReturn(afiSafiType).when(entryDep).getAfiSafType();

        advRoute = routeWithLocalPref(LOCAL_PREF);
        entryFactory = RibOutEntryFactory.newSharing();
    }

    /**
     * Two peers whose export policy changed nothing must store one instance.
     *
     * <p>Without the sharing each peer calls {@code RIBSupport.createRoute()} itself, which returns equal but
     * distinct entries, and {@code assertSame} below fails.
     */
    @Test
    public void unchangedRouteSharesOneEntryAcrossPeers() {
        exportUnchanged();

        peerA.refreshRibOut(entryDep, List.of(), List.of(advRoute), entryFactory);
        peerB.refreshRibOut(entryDep, List.of(), List.of(advRoute), entryFactory);

        assertSame("both peers must store the same AdjRibsOut entry for a route exported unchanged",
            capturePut(txA), capturePut(txB));
    }

    /**
     * A peer whose attributes the export policy rewrote must get an entry of its own.
     *
     * <p>Sharing a rewritten entry would make other peers advertise that peer's attributes.
     */
    @Test
    public void rewrittenRouteIsNotSharedAcrossPeers() {
        // Peer A keeps its attributes, peer B has its local preference rewritten.
        final var rewrittenFor = peerB.getPeerId();
        doAnswer(inv -> {
            final var params = inv.<BGPRouteEntryExportParameters>getArgument(0);
            final var attributes = inv.<Attributes>getArgument(1);
            return Optional.of(rewrittenFor.equals(params.getToPeerId())
                ? new AttributesBuilder(attributes)
                    .setLocalPref(new LocalPrefBuilder().setPref(REWRITTEN_LOCAL_PREF).build())
                    .build()
                : attributes);
        }).when(routingPolicies).applyExportPolicies(any(BGPRouteEntryExportParameters.class), any(Attributes.class),
            any(AfiSafiType.class));

        peerA.refreshRibOut(entryDep, List.of(), List.of(advRoute), entryFactory);
        peerB.refreshRibOut(entryDep, List.of(), List.of(advRoute), entryFactory);

        final var storedA = capturePut(txA);
        final var storedB = capturePut(txB);
        assertNotSame("a rewritten route must not be stored as the entry shared with the other peers",
            storedA, storedB);
        assertEquals("peer A keeps the attributes it was advertised with", LOCAL_PREF, localPrefOf(storedA));
        assertEquals("peer B gets the attributes its export policy produced", REWRITTEN_LOCAL_PREF,
            localPrefOf(storedB));
    }

    /**
     * A peer using add-path must get an entry of its own, because its key carries the path id.
     *
     * <p>Both peers see the same route and the same attributes, so only the key separates the two entries.
     */
    @Test
    public void addPathPeerGetsItsOwnEntry() {
        exportUnchanged();
        final var addPathPeer = bringUpPeer(new Ipv4AddressNoZone("127.0.0.4"), "4.4.4.4", true);
        final var addPathTx = attachCapturingChain(addPathPeer);

        peerA.refreshRibOut(entryDep, List.of(), List.of(advRoute), entryFactory);
        addPathPeer.refreshRibOut(entryDep, List.of(), List.of(advRoute), entryFactory);

        final var storedA = capturePut(txA);
        final var storedAddPath = capturePut(addPathTx);
        assertNotSame("the add-path peer needs its own entry, keyed by path id", storedA, storedAddPath);
        assertEquals(Uint32.ZERO, ribSupport.extractPathId(storedA.name()));
        assertEquals(PATH_ID, ribSupport.extractPathId(storedAddPath.name()));
    }

    /**
     * A factory used again after the route behind a key changed must rebuild instead of returning the entry it
     * kept for the previous route.
     *
     * <p>Without the check the second call below hands back the entry built from {@code LOCAL_PREF}.
     */
    @Test
    public void reusedFactoryRebuildsAfterRouteChanges() {
        final var changedRoute = routeWithLocalPref(REWRITTEN_LOCAL_PREF);
        final var key = advRoute.getNonAddPathRouteKeyIdentifier();

        final var firstEntry = entryFactory.entryFor(ribSupport, advRoute, key, advRoute.getAttributes());
        final var secondEntry = entryFactory.entryFor(ribSupport, changedRoute, key, changedRoute.getAttributes());

        assertNotSame("a factory used past its writes must not serve the entry for the previous route",
            firstEntry, secondEntry);
        assertEquals(REWRITTEN_LOCAL_PREF, localPrefOf(secondEntry));
    }

    /**
     * Returns the attributes unchanged, which is what the default policy does for the role pairs that only accept a
     * route. That is when sharing applies.
     */
    private void exportUnchanged() {
        doAnswer(inv -> Optional.of(inv.getArgument(1))).when(routingPolicies)
            .applyExportPolicies(any(BGPRouteEntryExportParameters.class), any(Attributes.class),
                any(AfiSafiType.class));
    }

    private BGPPeer bringUpPeer(final Ipv4AddressNoZone address, final String bgpId, final boolean addPath) {
        final var peer = AbstractAddPathTest.configurePeer(tableRegistry, address, getRib(), null, PeerRole.Ibgp,
            new StrictBGPPeerRegistry());
        // Enough of a BGPSession for BGPPeer.onSessionUp() to resolve the peer id and the add-path tables.
        final var session = mock(BGPSession.class);
        doReturn(Set.<BgpTableType>of()).when(session).getAdvertisedTableTypes();
        doReturn(new Ipv4Address(bgpId)).when(session).getBgpId();
        doReturn(addPath ? List.of(new AddressFamiliesBuilder()
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .setSendReceive(SendReceive.Both)
            .build()) : List.<AddressFamilies>of()).when(session).getAdvertisedAddPathTableTypes();
        doReturn(GracefulRestartUtil.EMPTY_GR_CAPABILITY).when(session).getAdvertisedGracefulRestartCapability();
        doReturn(GracefulRestartUtil.EMPTY_LLGR_CAPABILITY).when(session).getAdvertisedLlGracefulRestartCapability();
        peer.onSessionUp(session);
        return peer;
    }

    private DOMDataTreeWriteTransaction attachCapturingChain(final BGPPeer peer) {
        final var chain = mock(DOMTransactionChain.class);
        final var tx = mock(DOMDataTreeWriteTransaction.class);
        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doNothing().when(tx).put(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class),
            any(NormalizedNode.class));
        // Already complete, so the callback AbstractPeer registers just logs and there is nothing to stub.
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();
        doNothing().when(chain).close();
        peer.ribOutChain = chain;
        return tx;
    }

    private Uint32 localPrefOf(final MapEntryNode storedRoute) {
        final var attributes = ribSupport.attributeFromContainerNode(
            (ContainerNode) storedRoute.getChildByArg(ribSupport.routeAttributesIdentifier()));
        return attributes.getLocalPref().getPref();
    }

    private AdvertizedRoute<Ipv4RoutesCase, Ipv4Routes> routeWithLocalPref(final Uint32 localPref) {
        final var route = buildIpv4Route(localPref);
        return new AdvertizedRoute<>(ribSupport, route,
            (ContainerNode) route.getChildByArg(ribSupport.routeAttributesIdentifier()), SOURCE_PEER, false);
    }

    private MapEntryNode buildIpv4Route(final Uint32 localPref) {
        final var route = new Ipv4RouteBuilder()
            .setRouteKey(PREFIX)
            .setPathId(new PathId(PATH_ID))
            .setPrefix(new Ipv4Prefix(PREFIX))
            .setAttributes(new AttributesBuilder()
                .setLocalPref(new LocalPrefBuilder().setPref(localPref).build())
                .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
                .setAsPath(new AsPathBuilder().setSegments(List.of()).build())
                .build())
            .build();
        final var routes = new Ipv4RoutesBuilder().setIpv4Route(Map.of(route.key(), route)).build();

        final var routesId = DataObjectIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("rib")))
            .child(LocRib.class)
            .child(Tables.class, IPV4_UNICAST)
            .child(Ipv4RoutesCase.class, Ipv4Routes.class)
            .build();
        final var serializer = (BindingNormalizedNodeSerializer) mappingService.currentSerializer();
        final var routesNode = (ContainerNode) ((NodeResult) serializer.toNormalizedNode(routesId, routes)).node();
        final var routeMap = (MapNode) routesNode.getChildByArg(new NodeIdentifier(Ipv4Route.QNAME));
        return routeMap.body().iterator().next();
    }

    private static MapEntryNode capturePut(final DOMDataTreeWriteTransaction tx) {
        final var captor = ArgumentCaptor.forClass(MapEntryNode.class);
        verify(tx).put(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), captor.capture());
        return captor.getValue();
    }
}
