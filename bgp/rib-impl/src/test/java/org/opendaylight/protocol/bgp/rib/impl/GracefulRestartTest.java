/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkIdleState;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkRestartState;
import static org.opendaylight.protocol.bgp.rib.impl.CheckUtil.checkUpState;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
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

public class GracefulRestartTest extends AbstractAddPathTest {

    private BGPSessionImpl session;
    private BGPPeer peer;
    private final Set<TablesKey> afiSafiAdvertised = new HashSet<>();
    private final Set<TablesKey> gracefulAfiSafiAdvertised = new HashSet<>();
    private RIBImpl ribImpl;
    private Channel serverChannel;
    private SimpleSessionListener listener = new SimpleSessionListener();
    private final Ipv4Prefix PREFIX2 = new Ipv4Prefix("2.2.2.2/32");
    private final Ipv6Prefix PREFIX3 = new Ipv6Prefix("dead:beef::/64");
    private static final InstanceIdentifier<Tables> IPV4_IID = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("test-rib")))
            .child(LocRib.class)
            .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class))
            .build();
    private static final InstanceIdentifier<Tables> IPV6_IID = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("test-rib")))
            .child(LocRib.class)
            .child(Tables.class, new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class))
            .build();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Map<TablesKey, PathSelectionMode> pathTables
                = ImmutableMap.of(TABLES_KEY, new AllPathSelection());
        final ArrayList<BgpTableType> tableTypes = new ArrayList<>(TABLES_TYPE);
        tableTypes.add(new BgpTableTypeImpl(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        this.ribImpl = new RIBImpl(this.tableRegistry, new RibId("test-rib"), AS_NUMBER, BGP_ID,
                this.ribExtension, this.serverDispatcher, this.codecsRegistry,
                getDomBroker(), getDataBroker(), this.policies, tableTypes, pathTables);

        this.ribImpl.instantiateServiceInstance();
        this.ribImpl.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        final ChannelFuture channelFuture = this.serverDispatcher.createServer(new InetSocketAddress(RIB_ID, PORT));
        waitFutureSuccess(channelFuture);
        this.serverChannel = channelFuture.channel();

        gracefulAfiSafiAdvertised.add(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        afiSafiAdvertised.addAll(gracefulAfiSafiAdvertised);
        afiSafiAdvertised.add(new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        final BgpParameters parameters = createParameter(false, Collections.singletonMap(TABLES_KEY, true));
        this.peer = configurePeer(this.tableRegistry, PEER1, this.ribImpl, parameters, PeerRole.Ibgp,
                this.serverRegistry, afiSafiAdvertised, gracefulAfiSafiAdvertised);
        this.session = createPeerSession(PEER1, parameters, this.listener);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        waitFutureSuccess(this.serverChannel.close());
        super.tearDown();
    }

    /**
     * Test correct bahavior when connection restart is unnnoticed.
     * "Corrrect" means that the previous TCP session MUST be closed, and the new one retained.
     * Since the previous connection is considered to be terminated, no NOTIFICATION message should be sent.
     */
    @Test
    public void resetConnectionOnOpenTest() {
        final Open open = createClassicOpen(true);
        this.session.writeAndFlush(open);
        checkIdleState(this.peer);
        checkRestartState(this.peer, GRACEFUL_RESTART_TIME);
        assertEquals(0, this.listener.getListMsg().size());
    }

    /**
     * Test that routes from peer that has advertised the Graceful Restart Capability MUST be retained
     * for all the address families that were previously received in the Graceful Restart Capability.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void retainRoutesOnPeerRestartTest() throws Exception {
        final ArrayList<Ipv4Prefix> ipv4prefixes = new ArrayList<>();
        final ArrayList<Ipv6Prefix> ipv6prefixes = new ArrayList<>();
        ipv4prefixes.add(new Ipv4Prefix(PREFIX1));
        ipv4prefixes.add(new Ipv4Prefix(PREFIX2));
        ipv6prefixes.add(new Ipv6Prefix(PREFIX3));
        insertRoutes(ipv4prefixes, ipv6prefixes);
        checkLocRibIpv4(2);
        checkLocRibIpv6(1);

        this.session.close();
        checkIdleState(this.peer);
        checkLocRibIpv4(2);
        checkLocRibIpv6(0);
    }

    /**
     * If the session does not get re-established within the "Restart Time"
     * that the peer advertised previously, the Receiving Speaker MUST
     * delete all the stale routes from the peer that it is retaining.
     *
     * @throws Exception on reading Rib failure
     */
    @Test
    public void removeRoutesOnHlodTimeExpireTest() throws Exception {
        retainRoutesOnPeerRestartTest();
        checkRestartState(peer, GRACEFUL_RESTART_TIME);
        assertEquals(0, getLocalRib().getRib().get(0).getPeer().size());
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
        this.session = createPeerSession(PEER1, createParameter(false, null), this.listener);
        checkUpState(listener);
        checkLocRibIpv4(0);
        checkLocRibIpv6(0);
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
        this.session = createPeerSession(PEER1, createParameter(false, Collections.singletonMap(TABLES_KEY, false)),
                this.listener);
        checkUpState(listener);
        checkUpState(this.peer);
        checkLocRibIpv4(0);
        checkLocRibIpv6(0);
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
        this.session = createPeerSession(PEER1, createParameter(false, Collections.singletonMap(TABLES_KEY, true)),
                this.listener);
        checkUpState(listener);
        insertRoutes(Collections.singletonList(new Ipv4Prefix(PREFIX1)), null);
        insertRoutes(null, null);
        checkLocRibIpv4(1);
        checkLocRibIpv6(0);
    }

    private BgpRib getLocalRib() throws Exception {
        return readDataOperational(getDataBroker(), BGP_IID, bgpRib -> bgpRib);
    }

    private void checkLocRibIpv4(final int expectedRoutesOnDS) throws Exception {
        readDataOperational(getDataBroker(), IPV4_IID, table -> {
            int size = 0;
            final Ipv4RoutesCase routesCase = (Ipv4RoutesCase) table.getRoutes();
            if (routesCase != null && routesCase.getIpv4Routes() != null &&
                    routesCase.getIpv4Routes().getIpv4Route() != null) {
                size = routesCase.getIpv4Routes().getIpv4Route().size();
            }
            Assert.assertEquals(expectedRoutesOnDS, size);
            return table;
        });
    }

    private void checkLocRibIpv6(final int expectedRoutesOnDS) throws Exception {
        readDataOperational(getDataBroker(), IPV6_IID, table -> {
            int size = 0;
            final Ipv6RoutesCase routesCase = (Ipv6RoutesCase) table.getRoutes();
            if (routesCase != null && routesCase.getIpv6Routes() != null &&
                    routesCase.getIpv6Routes().getIpv6Route() != null) {
                size = routesCase.getIpv6Routes().getIpv6Route().size();
            }
            Assert.assertEquals(expectedRoutesOnDS, size);
            return table;
        });
    }

    private void insertRoutes(List<Ipv4Prefix> ipv4prefixes, List<Ipv6Prefix> ipv6prefixes) {
        if (ipv4prefixes ==null && ipv6prefixes == null) {
            waitFutureSuccess(this.session.writeAndFlush(PeerUtil.createEndOfRib(new TablesKey(Ipv4AddressFamily.class,
                    UnicastSubsequentAddressFamily.class))));
            waitFutureSuccess(this.session.writeAndFlush(PeerUtil.createEndOfRib(new TablesKey(Ipv6AddressFamily.class,
                    UnicastSubsequentAddressFamily.class))));
            return;
        }

        if (ipv4prefixes != null && !ipv4prefixes.isEmpty()) {
            final MpReachNlri reachIpv4 = PeerUtil.createMpReachNlri(new IpAddress(new Ipv4Address("127.0.0.2")), 0,
                    ipv4prefixes.stream()
                            .map(IpPrefix::new)
                            .collect(Collectors.toList()));
            final Update update1 = PeerUtil.createUpdate(BgpOrigin.Igp, Collections.emptyList(), 100, reachIpv4, null);
            waitFutureSuccess(this.session.writeAndFlush(update1));
        }

        if (ipv6prefixes != null && !ipv4prefixes.isEmpty()) {
            final MpReachNlri reachIpv6 = PeerUtil.createMpReachNlri(new IpAddress(new Ipv6Address("DEAD:BEEF::1")), 0,
                    ipv6prefixes.stream()
                            .map(IpPrefix::new)
                            .collect(Collectors.toList()));
            final Update update2 = PeerUtil.createUpdate(BgpOrigin.Igp, Collections.emptyList(), 100, reachIpv6, null);
            waitFutureSuccess(this.session.writeAndFlush(update2));
        }
    }

    private static Open createClassicOpen(boolean addGraceful) {
        final Map<TablesKey, Boolean> graceful = new HashMap<>();
        if (addGraceful) {
            graceful.put(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), true);
        }
        return new OpenBuilder()
                .setMyAsNumber((int) AS)
                .setHoldTimer(HOLDTIMER)
                .setVersion(new ProtocolVersion((short) 4))
                .setBgpParameters(Collections.singletonList(createParameter(false, graceful)))
                .setBgpIdentifier(PEER1)
                .build();
    }
}
