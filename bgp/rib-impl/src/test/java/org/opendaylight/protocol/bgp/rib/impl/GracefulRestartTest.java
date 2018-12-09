/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkStateIsNotRestarting;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkUpState;
import static org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.protocol.bgp.rib.impl.config.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public class GracefulRestartTest extends AbstractAddPathTest {

    private BGPSessionImpl session;
    private BGPPeer peer;
    private final Set<TablesKey> afiSafiAdvertised = new HashSet<>();
    private final Set<TablesKey> gracefulAfiSafiAdvertised = new HashSet<>();
    private RIBImpl ribImpl;
    private Channel serverChannel;
    private final SimpleSessionListener listener = new SimpleSessionListener();
    private final BgpParameters parameters = createParameter(false, true, Collections.singletonMap(TABLES_KEY, true));
    private static final int DEFERRAL_TIMER = 5;
    private static final RibId RIBID = new RibId("test-rib");
    private final Ipv4Prefix PREFIX2 = new Ipv4Prefix("2.2.2.2/32");
    private final Ipv6Prefix PREFIX3 = new Ipv6Prefix("dead:beef::/64");
    private final Ipv6Address IPV6_NEXT_HOP = new Ipv6Address("dead:beef::1");
    private static final TablesKey IPV6_TABLES_KEY = new TablesKey(Ipv6AddressFamily.class,
            UnicastSubsequentAddressFamily.class);

    private static final InstanceIdentifier<LocRib> LOC_RIB_IID = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(RIBID))
            .child(LocRib.class)
            .build();
    private static final InstanceIdentifier<Tables> IPV4_IID = LOC_RIB_IID.builder()
            .child(Tables.class,TABLES_KEY)
            .build();
    private static final InstanceIdentifier<Tables> IPV6_IID = LOC_RIB_IID.builder()
            .child(Tables.class, IPV6_TABLES_KEY)
            .build();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Map<TablesKey, PathSelectionMode> pathTables
                = ImmutableMap.of(TABLES_KEY, new AllPathSelection());
        final ArrayList<BgpTableType> tableTypes = new ArrayList<>(TABLES_TYPE);
        tableTypes.add(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        this.ribImpl = new RIBImpl(this.tableRegistry, RIBID, AS_NUMBER, BGP_ID, this.ribExtension,
                this.serverDispatcher, this.codecsRegistry,
                getDomBroker(), getDataBroker(), this.policies, tableTypes, pathTables);

        this.ribImpl.instantiateServiceInstance();
        this.ribImpl.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        final ChannelFuture channelFuture = this.serverDispatcher.createServer(new InetSocketAddress(RIB_ID, PORT));
        waitFutureSuccess(channelFuture);
        this.serverChannel = channelFuture.channel();

        gracefulAfiSafiAdvertised.add(TABLES_KEY);
        afiSafiAdvertised.add(TABLES_KEY);
        afiSafiAdvertised.add(IPV6_TABLES_KEY);
        final BgpPeer bgpPeer = Mockito.mock(BgpPeer.class);
        Mockito.doReturn(GRACEFUL_RESTART_TIME).when(bgpPeer).getGracefulRestartTimer();
        Mockito.doReturn(createParameter(false, true, Collections.singletonMap(TABLES_KEY, false))
                .getOptionalCapabilities()).when(bgpPeer).getBgpFixedCapabilities();
        this.peer = configurePeer(this.tableRegistry, PEER1, this.ribImpl, parameters, PeerRole.Ibgp,
                this.serverRegistry, afiSafiAdvertised, gracefulAfiSafiAdvertised, bgpPeer);
        this.session = createPeerSession(PEER1, parameters, this.listener);
    }

    @Override
    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        waitFutureSuccess(this.serverChannel.close());
        this.session.close();
        super.tearDown();
    }

    /**
     * Test correct behavior when connection restart is unnoticed.
     * "Correct" means that the previous TCP session MUST be closed, and the new one retained.
     * Since the previous connection is considered to be terminated, no NOTIFICATION message should be sent.
     */
    @Test
    public void resetConnectionOnOpenTest() {

        checkReceivedMessages(this.listener, 2);
        final Open open = createClassicOpen(true);
        this.session.writeAndFlush(open);
        checkIdleState(this.peer);
        checkReceivedMessages(this.listener, 2);
    }

    /**
     * Test that routes from peer that has advertised the Graceful Restart Capability MUST be retained
     * for all the address families that were previously received in the Graceful Restart Capability.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void retainRoutesOnPeerRestartTest() throws Exception {
        final List<Ipv4Prefix> ipv4Prefixes = Arrays.asList(new Ipv4Prefix(PREFIX1), new Ipv4Prefix(PREFIX2));
        final List<Ipv6Prefix> ipv6Prefixes = Collections.singletonList(new Ipv6Prefix(PREFIX3));
        insertRoutes(ipv4Prefixes, ipv6Prefixes);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(1);

        this.session.close();
        checkIdleState(this.peer);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(0);
    }

    /**
     * If the session does not get re-established within the "Restart Time"
     * that the peer advertised previously, the Receiving Speaker MUST
     * delete all the stale routes from the peer that it is retaining.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void removeRoutesOnHoldTimeExpireTest() throws Exception {
        retainRoutesOnPeerRestartTest();
        checkStateIsNotRestarting(peer, GRACEFUL_RESTART_TIME);
        checkLocRibIpv4Routes(0);
        checkLocRibIpv6Routes(0);
    }

    /**
     * Once the session is re-established, if the Graceful
     * Restart Capability is not received in the re-established session at
     * all, then the Receiving Speaker MUST immediately remove all the stale
     * routes from the peer that it is retaining for that address family.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void removeRoutesOnMissingGracefulRestartTest() throws Exception {
        retainRoutesOnPeerRestartTest();
        this.session = createPeerSession(PEER1, createParameter(false, true, null), this.listener);
        checkUpState(listener);
        checkLocRibIpv4Routes(0);
        checkLocRibIpv6Routes(0);
    }

    /**
     * Once the session is re-established, if a specific address family is not included
     * in the newly received Graceful Restart Capability, then the Receiving Speaker
     * MUST immediately remove all the stale routes from the peer that it is retaining
     * for that address family.
     *
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void removeRoutesOnMissingGracefulRestartAfiSafiTest() throws Exception {
        retainRoutesOnPeerRestartTest();
        this.session = createPeerSession(PEER1, createParameter(false, true,
                Collections.singletonMap(TABLES_KEY, false)), this.listener);
        checkUpState(listener);
        checkUpState(this.peer);
        checkLocRibIpv4Routes(0);
        checkLocRibIpv6Routes(0);
    }

    /**
     * Once the End-of-RIB marker for an address family is received from the peer, it MUST
     * immediately remove any routes from the peer that are still marked as stale for that
     * address family.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void removeStaleRoutesAfterRestartTest() throws Exception {
        retainRoutesOnPeerRestartTest();
        this.session = createPeerSession(PEER1, createParameter(false, true,
                Collections.singletonMap(TABLES_KEY, true)), this.listener);
        checkUpState(this.listener);
        final List<Ipv4Prefix> ipv4prefixes = Arrays.asList(new Ipv4Prefix(PREFIX1));
        insertRoutes(ipv4prefixes, null);
        insertRoutes(null, null);
        checkLocRibIpv4Routes(1);
        checkLocRibIpv6Routes(0);
    }

    /**
     * Perform local graceful restart and verify routes are preserved.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void performLocalGracefulRestart() throws Exception {
        final List<Ipv4Prefix> ipv4prefixes = Arrays.asList(new Ipv4Prefix(PREFIX1), new Ipv4Prefix(PREFIX2));
        final List<Ipv6Prefix> ipv6prefixes = Arrays.asList(new Ipv6Prefix(PREFIX3));
        insertRoutes(ipv4prefixes, ipv6prefixes);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(1);

        this.peer.restartGracefully(DEFERRAL_TIMER).get();
        this.session = createPeerSession(PEER1, this.parameters, this.listener);
        checkUpState(this.listener);
        checkUpState(this.peer);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(0);
    }

    /**
     * Wait with route selection until EOT is received.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void waitForEORonLocalGracefulRestart() throws Exception {
        performLocalGracefulRestart();
        final List<Ipv4Prefix> ipv4prefixes = Arrays.asList(new Ipv4Prefix(PREFIX1));
        final List<Ipv6Prefix> ipv6prefixes = Arrays.asList(new Ipv6Prefix(PREFIX3));
        insertRoutes(ipv4prefixes, ipv6prefixes);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(0);
        insertRoutes(null, null);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(1);
    }

    /**
     * Wait with route selection until deferral time is expired.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void waitForDeferralTimerOnLocalGracefulRestart() throws Exception {
        performLocalGracefulRestart();
        final List<Ipv4Prefix> ipv4prefixes = Arrays.asList(new Ipv4Prefix(PREFIX1));
        final List<Ipv6Prefix> ipv6prefixes = Arrays.asList(new Ipv6Prefix(PREFIX3));
        insertRoutes(ipv4prefixes, ipv6prefixes);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(0);
        checkStateIsNotRestarting(this.peer, DEFERRAL_TIMER);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(1);
    }

    /**
     * After graceful restart is performed from peer side we have to re-advertise routes followed by
     * End-of-RIB marker.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void verifySendEORafterRestartTest() throws Exception {
        final SimpleSessionListener listener2 = new SimpleSessionListener();
        configurePeer(this.tableRegistry, PEER2, this.ribImpl, this.parameters, PeerRole.Ebgp,
                this.serverRegistry, afiSafiAdvertised, gracefulAfiSafiAdvertised);
        final BGPSessionImpl session2 = createPeerSession(PEER2, this.parameters, listener2);
        final List<Ipv4Prefix> ipv4Prefixes = Arrays.asList(new Ipv4Prefix(PREFIX1));
        final List<Ipv4Prefix> ipv4Prefixes2 = Arrays.asList(new Ipv4Prefix(PREFIX2));
        final List<Ipv6Prefix> ipv6Prefixes = Collections.singletonList(new Ipv6Prefix(PREFIX3));
        insertRoutes(ipv4Prefixes, ipv6Prefixes);
        insertRoutes(ipv4Prefixes2, PEER2, null, null, session2, BgpOrigin.Egp);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(1);
        org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages(this.listener, 3);
        // verify sending of Ipv4 EOT, Ipv6 EOT and Ipv4 update with route
        checkReceivedMessages(this.listener, 3);
        assertTrue(this.listener.getListMsg().get(0) instanceof Update);
        assertTrue(BgpPeerUtil.isEndOfRib((Update)this.listener.getListMsg().get(0)));
        assertTrue(this.listener.getListMsg().get(1) instanceof Update);
        assertTrue(BgpPeerUtil.isEndOfRib((Update)this.listener.getListMsg().get(1)));
        assertTrue(this.listener.getListMsg().get(2) instanceof Update);
        assertFalse(BgpPeerUtil.isEndOfRib((Update)this.listener.getListMsg().get(2)));

        this.session.close();
        checkIdleState(this.peer);
        checkLocRibIpv4Routes(2);
        checkLocRibIpv6Routes(0);
        // verify nothing new was sent
        checkReceivedMessages(this.listener, 3);

        this.session = createPeerSession(PEER1, createParameter(false, true,
                Collections.singletonMap(TABLES_KEY, true)), this.listener);
        checkUpState(listener);
        checkUpState(this.peer);
        org.opendaylight.protocol.util.CheckUtil.checkReceivedMessages(this.listener, 6);
        // verify sending of Ipv4 update with route, Ipv4 EOT and Ipv6 EOT; order can vary based on ODTC order
        final List<Notification> subList = this.listener.getListMsg().subList(3, 6);
        int EOTCount = 0;
        int routeUpdateCount = 0;
        for (Notification message : subList) {
            if (BgpPeerUtil.isEndOfRib((Update) message)) {
                EOTCount++;
            } else {
                routeUpdateCount++;
            }
        }
        assertEquals(2, EOTCount);
        assertEquals(1, routeUpdateCount);
    }

    private void checkLocRibIpv4Routes(final int expectedRoutesOnDS) throws Exception {
        readDataOperational(getDataBroker(), IPV4_IID, table -> {
            int size = 0;
            final Ipv4RoutesCase routesCase = (Ipv4RoutesCase) table.getRoutes();
            if (routesCase != null && routesCase.getIpv4Routes() != null &&
                    routesCase.getIpv4Routes().getIpv4Route() != null) {
                size = routesCase.getIpv4Routes().getIpv4Route().size();
            }
            assertEquals(expectedRoutesOnDS, size);
            return table;
        });
    }

    private void checkLocRibIpv6Routes(final int expectedRoutesOnDS) throws Exception {
        readDataOperational(getDataBroker(), IPV6_IID, table -> {
            int size = 0;
            final Ipv6RoutesCase routesCase = (Ipv6RoutesCase) table.getRoutes();
            if (routesCase != null && routesCase.getIpv6Routes() != null &&
                    routesCase.getIpv6Routes().getIpv6Route() != null) {
                size = routesCase.getIpv6Routes().getIpv6Route().size();
            }
            assertEquals(expectedRoutesOnDS, size);
            return table;
        });
    }

    private void insertRoutes(final List<Ipv4Prefix> ipv4prefixes, final List<Ipv6Prefix> ipv6prefixes) {
        insertRoutes(ipv4prefixes, PEER1, ipv6prefixes, IPV6_NEXT_HOP, this.session, BgpOrigin.Igp);
    }

    private static void insertRoutes(final List<Ipv4Prefix> ipv4prefixes, final Ipv4Address ipv4NeighborAddress,
                              final List<Ipv6Prefix> ipv6prefixes, final Ipv6Address ipv6NeighborAddress,
                              final BGPSessionImpl session, final BgpOrigin peerRole) {
        if (ipv4prefixes == null && ipv6prefixes == null) {
            waitFutureSuccess(session.writeAndFlush(BgpPeerUtil.createEndOfRib(TABLES_KEY)));
            waitFutureSuccess(session.writeAndFlush(BgpPeerUtil.createEndOfRib(IPV6_TABLES_KEY)));
            return;
        }

        if (ipv4prefixes != null && !ipv4prefixes.isEmpty()) {
            final MpReachNlri reachIpv4 = PeerUtil.createMpReachNlri(new IpAddress(ipv4NeighborAddress),
                    ipv4prefixes.stream()
                            .map(IpPrefix::new)
                            .collect(Collectors.toList()));
            final Update update1 = PeerUtil.createUpdate(peerRole, Collections.emptyList(), 100, reachIpv4, null);
            waitFutureSuccess(session.writeAndFlush(update1));
        }

        if (ipv6prefixes != null && !ipv4prefixes.isEmpty()) {
            final MpReachNlri reachIpv6 = PeerUtil.createMpReachNlri(new IpAddress(ipv6NeighborAddress),
                    ipv6prefixes.stream()
                            .map(IpPrefix::new)
                            .collect(Collectors.toList()));
            final Update update2 = PeerUtil.createUpdate(peerRole, Collections.emptyList(), 100, reachIpv6, null);
            waitFutureSuccess(session.writeAndFlush(update2));
        }
    }

    private static Open createClassicOpen(final boolean addGraceful) {
        final Map<TablesKey, Boolean> graceful = new HashMap<>();
        if (addGraceful) {
            graceful.put(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), true);
        }
        return new OpenBuilder()
                .setMyAsNumber((int) AS)
                .setHoldTimer(HOLDTIMER)
                .setVersion(new ProtocolVersion((short) 4))
                .setBgpParameters(Collections.singletonList(createParameter(false, true, graceful)))
                .setBgpIdentifier(PEER1)
                .build();
    }
}
