/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.EFFRIBIN_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.PEER_NID;
import static org.opendaylight.protocol.bgp.rib.spi.RIBNodeIdentifiers.TABLES_NID;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker.DataTreeChangeExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.inet.RIBActivator;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.protocol.bgp.rib.spi.BGPPeerTracker;
import org.opendaylight.protocol.bgp.rib.spi.IdentifierUtils;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryDependenciesContainer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.ChildOf;
import org.opendaylight.yangtools.binding.ChoiceIn;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer.NodeResult;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.spi.DataTreeCandidates;

/**
 * Reproduces the lost initial advertisement to a freshly connected peer. {@code LocRibWriter} dumps the existing
 * loc-rib routes to a new peer only when it processes that peer's effective-rib-in table creation and the peer is
 * already present in the peer tracker. When the table-creation event is processed before the peer registers, the
 * lookup returns null, the dump is skipped and never retried, so the peer never receives the routes that already
 * existed when it connected.
 */
public class LocRibWriterNewPeerInitTest extends AbstractRIBTestSetup {
    private static final TablesKey TABLES_KEY =
        new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);
    private static final String PREFIX = "1.1.1.0/24";
    private static final PeerId PEER_A = new PeerId("bgp://127.0.0.2");
    private static final PeerId PEER_B = new PeerId("bgp://127.0.0.3");
    private static final AsNumber AS_NUMBER = new AsNumber(Uint32.valueOf(AS));

    private final BGPPeerTracker peerTracker = new BGPPeerTrackerImpl();

    private RIBExtensionProviderContext ribContext;
    private YangInstanceIdentifier ribIId;
    private MapEntryNode tableWithRoute;
    private Peer peerB;
    private DOMTransactionChain chain;
    private DataTreeChangeExtension dataBroker;

    @Before
    public void setUpWriter() {
        ribContext = new SimpleRIBExtensionProviderContext();
        new RIBActivator().startRIBExtensionProvider(ribContext, mappingService.currentSerializer());
        ribIId = getRib().getYangRibId();
        tableWithRoute = buildTableWithRoute();

        peerB = mock(Peer.class);
        doReturn(PEER_B).when(peerB).getPeerId();
        doReturn(PeerRole.RrClient).when(peerB).getRole();
        doReturn(true).when(peerB).supportsTable(any(TablesKey.class));
        doNothing().when(peerB).initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());

        final DOMDataTreeWriteTransaction tx = mock(DOMDataTreeWriteTransaction.class);
        doNothing().when(tx).put(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode.class));
        doNothing().when(tx).merge(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class),
            any(org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode.class));
        doNothing().when(tx).delete(any(LogicalDatastoreType.class), any(YangInstanceIdentifier.class));
        doReturn(CommitInfo.emptyFluentFuture()).when(tx).commit();

        chain = mock(DOMTransactionChain.class);
        doReturn(tx).when(chain).newWriteOnlyTransaction();
        doNothing().when(chain).close();

        final Registration reg = mock(Registration.class);
        doNothing().when(reg).close();
        dataBroker = mock(DataTreeChangeExtension.class);
        doReturn(reg).when(dataBroker).registerTreeChangeListener(any(DOMDataTreeIdentifier.class),
            any(DOMDataTreeChangeListener.class));
    }

    /**
     * Test that a peer which registers right after its table-creation event still receives the existing routes.
     *
     * <p>The method is generic so the route support and the loc-rib writer use the same route type parameters.
     */
    @Test
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>>
            void newPeerMustReceiveExistingRoutesWhenRegisteredAfterTableEvent() {
        final RIBSupport<C, S> ribSupport = ribContext.getRIBSupport(TABLES_KEY);
        try (LocRibWriter<C, S> locRibWriter = LocRibWriter.create(ribSupport, IPV4UNICAST.VALUE, chain, ribIId,
                AS_NUMBER, dataBroker, policies, peerTracker,
                BasePathSelectionModeFactory.createBestPathSelectionStrategy())) {
            // PeerA already advertised a route, so the loc-rib holds a best path.
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_A, tableWithRoute)));
            // PeerB connects. Its empty effective-rib-in table is processed before PeerB registers in the tracker.
            locRibWriter.onDataTreeChanged(List.of(effRibInEvent(PEER_B, ribSupport.emptyTable())));
            // PeerB registers slightly later. The registration is intentionally not closed, the tracker is per-test.
            peerTracker.registerPeer(peerB);

            // PeerB must still receive the route that existed when it connected.
            verify(peerB).initializeRibOut(any(RouteEntryDependenciesContainer.class), anyList());
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
