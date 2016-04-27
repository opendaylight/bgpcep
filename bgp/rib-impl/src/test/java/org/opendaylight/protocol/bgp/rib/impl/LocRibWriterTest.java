/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIBSupportContextRegistry;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.AdjRibOut;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.peer.EffectiveRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class LocRibWriterTest extends AbstractDataBrokerTest implements TransactionChainListener {

    private static final Ipv4Prefix PREFIX = new Ipv4Prefix("1.1.1.0/24");
    private static final RibId RIB_ID = new RibId("rib");
    private static final Ipv4Address BGP_ID = new Ipv4Address("10.25.2.1");
    private static final Ipv4Address BGP_ID2 = new Ipv4Address("10.25.2.2");
    private static final PeerKey PEER_KEY = new PeerKey(RouterIds.createPeerId(BGP_ID));
    private static final PeerKey PEER_KEY2 = new PeerKey(RouterIds.createPeerId(BGP_ID2));
    private static final InstanceIdentifier<Rib> RIB_IID = InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(RIB_ID)).build();
    private static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    private static final InstanceIdentifier EFF_IID = RIB_IID
            .child(Peer.class, PEER_KEY).child(EffectiveRibIn.class).child(Tables.class, TABLES_KEY);
    private static final InstanceIdentifier<Ipv4Routes> EFF_ROUTES_IID = EFF_IID.child(Ipv4Routes.class);

    private static final InstanceIdentifier EFF_IID2 = RIB_IID
            .child(Peer.class, PEER_KEY2).child(EffectiveRibIn.class).child(Tables.class, TABLES_KEY);
    private static final InstanceIdentifier<Ipv4Routes> EFF_ROUTES_IID2 = EFF_IID2.child(Ipv4Routes.class);
    private static final InstanceIdentifier RIB_OUT_IID = RIB_IID
            .child(Peer.class, PEER_KEY2).child(AdjRibOut.class).child(Tables.class, TABLES_KEY);
    private static final InstanceIdentifier<Ipv4Routes> RIB_OUT_ROUTES_IID = RIB_OUT_IID.child(Ipv4Routes.class);

    private static final YangInstanceIdentifier PEER1_IID = createYangIId(PEER_KEY);
    private static final YangInstanceIdentifier PEER2_IID = createYangIId(PEER_KEY2);

    private final PolicyDatabase pd = new PolicyDatabase((long) 35, BGP_ID, new ClusterIdentifier(BGP_ID));

    private LocRibWriter locRibWriter;
    private RIBActivator ribActivator;
    private ExportPolicyPeerTracker exportPolicyPeerTracker;

    @Before
    public void setUp() throws Exception {
        final DataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        final BindingToNormalizedNodeCodec codecFactory = customizer.getBindingToNormalized();

        final SimpleRIBExtensionProviderContext extensions = new SimpleRIBExtensionProviderContext();
        this.ribActivator = new RIBActivator();
        this.ribActivator.startRIBExtensionProvider(extensions);
        final RIBSupportContextRegistry registry = RIBSupportContextRegistryImpl.create(
                extensions, CodecsRegistryImpl.create(codecFactory, extensions.getClassLoadingStrategy()));

        final LocRib locRib = new LocRibBuilder().setTables(Collections.<Tables>emptyList()).build();
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, RIB_IID.child(LocRib.class), locRib, true);
        wTx.submit().checkedGet();

        final DOMTransactionChain txChain = getDomBroker().createTransactionChain(this);
        final DOMDataTreeChangeService service = (DOMDataTreeChangeService) getDomBroker().getSupportedExtensions().get(DOMDataTreeChangeService.class);
        this.exportPolicyPeerTracker = new ExportPolicyPeerTracker(this.pd);
        this.exportPolicyPeerTracker.addToExportPolicies(PEER_KEY.getPeerId(), PEER1_IID, PeerRole.Ibgp, Collections.singleton(TABLES_KEY));
        this.locRibWriter = LocRibWriter.create(registry, TABLES_KEY, txChain,
                YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Rib.QNAME).nodeWithKey(Rib.QNAME, RIBImpl.RIB_ID_QNAME, RIB_ID.getValue()).build(),
                new AsNumber((long) 35), service, this.exportPolicyPeerTracker, new CacheDisconnectedPeersImpl());
    }

    @After
    public void tearDown() {
        this.locRibWriter.close();
        this.ribActivator.close();
    }

    @Test
    public void testSingleRouteWrite() throws TransactionCommitFailedException, ReadFailedException, InterruptedException {
        writeIpv4RouteToEffRib(PREFIX, BGP_ID);
        final Ipv4Route locRibRoute = getLocRibIpv4Routes(getLocRibRoutes().getTables().get(0).getRoutes()).get(0);
        Assert.assertEquals(PREFIX, locRibRoute.getPrefix());
    }

    @Test
    public void testSingleRouteRemove() throws TransactionCommitFailedException, ReadFailedException, InterruptedException {
        writeIpv4RouteToEffRib(PREFIX, BGP_ID);
        removeRouteFromEffRib(PREFIX);
        final List<Ipv4Route> locRibIpv4Routes = getLocRibIpv4Routes(getLocRibRoutes().getTables().get(0).getRoutes());
        Assert.assertTrue(locRibIpv4Routes.isEmpty());
    }

    @Test
    public void testSingleRouteReadvertise() throws TransactionCommitFailedException, ReadFailedException, InterruptedException {
        createSecondPeer();
        writeIpv4RouteToEffRib(PREFIX, BGP_ID);
        final Ipv4Route ipv4Route = getRibOutIpv4Routes(getPeerRibOut(PEER_KEY2).getTables().get(0).getRoutes()).get(0);
        Assert.assertEquals(PREFIX, ipv4Route.getPrefix());
    }

    @Test
    public void testInitialRoutesDump() throws TransactionCommitFailedException, ReadFailedException, InterruptedException {
        writeIpv4RouteToEffRib(PREFIX, BGP_ID);
        createSecondPeer();
        final Ipv4Route ipv4Route = getRibOutIpv4Routes(getPeerRibOut(PEER_KEY2).getTables().get(0).getRoutes()).get(0);
        Assert.assertEquals(PREFIX, ipv4Route.getPrefix());
    }

    private void writeIpv4RouteToEffRib(final Ipv4Prefix prefix, final Ipv4Address nextHop) throws TransactionCommitFailedException {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        final Ipv4Route ipv4Route = new Ipv4RouteBuilder().setPrefix(prefix).setAttributes(new AttributesBuilder().setCNextHop(
                new Ipv4NextHopCaseBuilder().setIpv4NextHop(
                        new Ipv4NextHopBuilder().setGlobal(nextHop).build()).build()).build()).build();
        wTx.put(LogicalDatastoreType.OPERATIONAL, EFF_ROUTES_IID.child(Ipv4Route.class, ipv4Route.getKey()), ipv4Route, true);
        wTx.submit().checkedGet();
    }

    private void removeRouteFromEffRib(final Ipv4Prefix prefix) throws TransactionCommitFailedException {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, EFF_ROUTES_IID.child(Ipv4Route.class, new Ipv4RouteKey(prefix)));
        wTx.submit().checkedGet();
    }

    private void createSecondPeer() throws TransactionCommitFailedException {
        this.exportPolicyPeerTracker.addToExportPolicies(PEER_KEY2.getPeerId(), PEER2_IID, PeerRole.Ebgp, Collections.singleton(TABLES_KEY));
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, EFF_ROUTES_IID2, new Ipv4RoutesBuilder().setIpv4Route(Collections.<Ipv4Route>emptyList()).build(), true);
        wTx.put(LogicalDatastoreType.OPERATIONAL, RIB_OUT_ROUTES_IID, new Ipv4RoutesBuilder().setIpv4Route(Collections.<Ipv4Route>emptyList()).build(), true);
        wTx.submit().checkedGet();
    }

    private static List<Ipv4Route> getLocRibIpv4Routes(final Routes routes) {
        return ((Ipv4RoutesCase)routes).getIpv4Routes().getIpv4Route();
    }

    private static List<Ipv4Route> getRibOutIpv4Routes(final Routes routes) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.peer.adj.rib.out.tables.routes.Ipv4RoutesCase)
                routes).getIpv4Routes().getIpv4Route();
    }

    private LocRib getLocRibRoutes() throws ReadFailedException, InterruptedException {
        Thread.sleep(500);
        try (final ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction()) {
            return rTx.read(LogicalDatastoreType.OPERATIONAL, RIB_IID.child(LocRib.class)).checkedGet().get();
        }
    }

    private AdjRibOut getPeerRibOut(final PeerKey peerKey) throws ReadFailedException, InterruptedException {
        Thread.sleep(500);
        try (final ReadOnlyTransaction rTx = getDataBroker().newReadOnlyTransaction()) {
            return rTx.read(LogicalDatastoreType.OPERATIONAL, RIB_IID.child(Peer.class, peerKey).child(AdjRibOut.class)).checkedGet().get();
        }
    }

    private static YangInstanceIdentifier createYangIId(final PeerKey peerKey) {
        return YangInstanceIdentifier.builder().node(BgpRib.QNAME).node(Rib.QNAME)
                .nodeWithKey(Rib.QNAME, RIBImpl.RIB_ID_QNAME, RIB_ID.getValue()).node(Peer.QNAME).node(IdentifierUtils.domPeerId(peerKey.getPeerId())).build();
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        Assert.fail(cause.getMessage());
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
    }
}
