/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;
import static org.opendaylight.protocol.util.CheckUtil.waitFutureSuccess;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.peer.adj.rib.in.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.peer.adj.rib.in.tables.routes.Ipv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.prefixes.destination.ipv4.Ipv4PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.PeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.peer.AdjRibIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv6NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv6.next.hop._case.Ipv6NextHopBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class GracefulRestartTest extends AbstractAddPathTest {

    private final IpAddress neighborAddress = new IpAddress(new Ipv4Address("127.0.0.1"));
    private BGPSessionImpl session;
    private Map<YangInstanceIdentifier, NormalizedNode<?, ?>> routes;
    private BGPPeer peer;
    private final Set<TablesKey> afiSafiAdvertised = new HashSet<>();
    private final Set<TablesKey> gracefulAfiSafiAdvertised = new HashSet<>();
    private static final int RESTART_TIME = 5;
    private RIBImpl ribImpl;
    private Channel serverChannel;
    private SimpleSessionListener listener = new SimpleSessionListener();
    private final Ipv4Prefix PREFIX2 = new Ipv4Prefix("2.2.2.2/32");
    private final Ipv6Prefix PREFIX3 = new Ipv6Prefix("dead:beef::/64");
    private static final InstanceIdentifier<Peer> PEER_IID = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId("test-rib")))
            .child(Peer.class, new PeerKey(new PeerId(new Uri("bgp://" + PEER1.getValue()))))
            .build();
    private final static InstanceIdentifier<Tables> IPV4_IID = PEER_IID.builder()
            .child(AdjRibIn.class)
            .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class))
            .build();
    private final static InstanceIdentifier<Tables> IPV6_IID = PEER_IID.builder()
            .child(AdjRibIn.class)
            .child(Tables.class, new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class))
            .build();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Map<TablesKey, PathSelectionMode> pathTables
                = ImmutableMap.of(TABLES_KEY, new AllPathSelection());

        this.ribImpl = new RIBImpl(this.tableRegistry, new RibId("test-rib"), AS_NUMBER, BGP_ID,
                this.ribExtension, this.serverDispatcher, this.codecsRegistry,
                getDomBroker(), getDataBroker(), this.policies, TABLES_TYPE, pathTables);

        this.ribImpl.instantiateServiceInstance();
        this.ribImpl.onGlobalContextUpdated(this.schemaService.getGlobalContext());
        final ChannelFuture channelFuture = this.serverDispatcher.createServer(new InetSocketAddress(RIB_ID, PORT));
        waitFutureSuccess(channelFuture);
        this.serverChannel = channelFuture.channel();

        gracefulAfiSafiAdvertised.add(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        afiSafiAdvertised.addAll(gracefulAfiSafiAdvertised);
        afiSafiAdvertised.add(new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class));
        BgpParameters parameters = createParameter(true, true);
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
        Open open = createClassicOpen(true);
        this.session.writeAndFlush(open);
        CheckUtil.checkIdleState(this.peer);
        CheckUtil.checkRestartState(this.peer, RESTART_TIME);
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
        ArrayList<Ipv4Prefixes> ipv4prefixes = new ArrayList<>();
        ArrayList<Ipv6Prefixes> ipv6prefixes = new ArrayList<>();
        ipv4prefixes.add(new Ipv4PrefixesBuilder()
                .setPrefix(new Ipv4Prefix(PREFIX1)).build());
        ipv4prefixes.add(new Ipv4PrefixesBuilder()
                .setPrefix(new Ipv4Prefix(PREFIX2)).build());
        ipv6prefixes.add(new Ipv6PrefixesBuilder()
                .setPrefix(new Ipv6Prefix(PREFIX3)).build());
        insertRoutes(ipv4prefixes, ipv6prefixes);
        checkLocRib(IPV4_IID, 2, true);
        checkLocRib(IPV6_IID, 1, false);

        this.session.close();
        CheckUtil.checkIdleState(this.peer);
        checkLocRib(IPV4_IID, 2, true);
        checkLocRib(IPV6_IID, 0, false);
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
        CheckUtil.checkRestartState(peer, RESTART_TIME);
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
        this.session = createPeerSession(PEER1, createParameter(false, false), this.listener);
        CheckUtil.checkUpState(listener);
        checkLocRib(IPV4_IID, 0, true);
        checkLocRib(IPV6_IID, 0, false);
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
        this.session = createPeerSession(PEER1, createParameter(true, false), this.listener);
        CheckUtil.checkUpState(listener);
        checkLocRib(IPV4_IID, 0, true);
        checkLocRib(IPV6_IID, 0, false);
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
        this.session = createPeerSession(PEER1, createParameter(true, false), this.listener);
        CheckUtil.checkUpState(listener);
        insertRoutes(Collections.singletonList(new Ipv4PrefixesBuilder()
                .setPrefix(new Ipv4Prefix(PREFIX1)).build()), null);
        insertRoutes(null, null);
        checkLocRib(IPV4_IID, 1, true);
        checkLocRib(IPV6_IID, 0, false);
    }

    private BgpRib getLocalRib() throws Exception {
        return readDataOperational(getDataBroker(), BGP_IID, bgpRib -> bgpRib);
    }

    private void checkLocRib(final InstanceIdentifier<Tables> iid, final int expectedRoutesOnDS, boolean ipv4)
            throws Exception {
        readDataOperational(getDataBroker(), iid, table -> {
            if (ipv4) {
                Assert.assertEquals(expectedRoutesOnDS, ((Ipv4RoutesCase)table.getRoutes())
                        .getIpv4Routes().getIpv4Route().size());
            } else {
                Assert.assertEquals(expectedRoutesOnDS, ((Ipv6RoutesCase)table.getRoutes())
                        .getIpv6Routes().getIpv6Route().size());
            }
            return table;
        });
    }

    private BGPPeer configurePeer(final BGPTableTypeRegistryConsumer tableRegistry,
                                 final Ipv4Address peerAddress, final RIBImpl ribImpl, final BgpParameters bgpParameters,
                                 final PeerRole peerRole, final BGPPeerRegistry bgpPeerRegistry,
                                 final Set<TablesKey> afiSafiAdvertised,
                                 final Set<TablesKey> gracefulAfiSafiAdvertised) {
        final IpAddress ipAddress = new IpAddress(peerAddress);

        final BGPPeer bgpPeer = new BGPPeer(tableRegistry, new IpAddress(peerAddress), ribImpl, peerRole,
                null, afiSafiAdvertised, gracefulAfiSafiAdvertised);
        final List<BgpParameters> tlvs = Lists.newArrayList(bgpParameters);
        bgpPeerRegistry.addPeer(ipAddress, bgpPeer,
                new BGPSessionPreferences(AS_NUMBER, HOLDTIMER, new BgpId(RIB_ID), AS_NUMBER, tlvs));
        bgpPeer.instantiateServiceInstance();
        return bgpPeer;
    }

    private void insertRoutes(List<Ipv4Prefixes> ipv4prefixes, List<Ipv6Prefixes> ipv6prefixes) {
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final LocalPref localPref = new LocalPrefBuilder().setPref((long) 100).build();
        final Attributes ab = new AttributesBuilder()
                .setOrigin(origin).setAsPath(asPath).setLocalPref(localPref).build();

        if (ipv4prefixes != null && !ipv4prefixes.isEmpty()) {
            final MpReachNlri n1 = new MpReachNlriBuilder()
                    .setAfi(Ipv4AddressFamily.class)
                    .setSafi(UnicastSubsequentAddressFamily.class)
                    .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                            .setDestinationType(new DestinationIpv4CaseBuilder()
                                    .setDestinationIpv4(new DestinationIpv4Builder()
                                            .setIpv4Prefixes(ipv4prefixes)
                                            .build()).build()).build()).build();
            final Update ud1 = new UpdateBuilder()
                    .setAttributes(new AttributesBuilder(ab)
                            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder()
                                    .setGlobal(new Ipv4Address("127.0.0.2")).build()).build())
                            .addAugmentation(Attributes1.class, new Attributes1Builder()
                                    .setMpReachNlri(n1).build()).build()).build();

            waitFutureSuccess(this.session.writeAndFlush(ud1));
        }
        if (ipv6prefixes != null && !ipv4prefixes.isEmpty()) {
            final MpReachNlri n2 = new MpReachNlriBuilder()
                    .setAfi(Ipv6AddressFamily.class)
                    .setSafi(UnicastSubsequentAddressFamily.class)
                    .setCNextHop(new Ipv6NextHopCaseBuilder()
                            .setIpv6NextHop(new Ipv6NextHopBuilder()
                                    .setGlobal(new Ipv6Address("DEAD:BABE::1"))
                                    .setLinkLocal(new Ipv6Address("fe80::c001:bff:fe7e:0"))
                                    .build()).build())
                    .setAdvertizedRoutes(new AdvertizedRoutesBuilder()
                            .setDestinationType(new DestinationIpv6CaseBuilder()
                                    .setDestinationIpv6(new DestinationIpv6Builder()
                                            .setIpv6Prefixes(Collections.singletonList((new Ipv6PrefixesBuilder()
                                                    .setPrefix(PREFIX3)
                                                    .build()))).build()).build()).build()).build();
            final Update ud2 = new UpdateBuilder()
                    .setAttributes(new AttributesBuilder(ab)
                            .addAugmentation(Attributes1.class, new Attributes1Builder()
                                    .setMpReachNlri(n2).build()).build()).build();

            waitFutureSuccess(this.session.writeAndFlush(ud2));
        }

        if (ipv4prefixes ==null && ipv6prefixes == null) {
            waitFutureSuccess(this.session.writeAndFlush(new UpdateBuilder().build()));

            final Update ud =  new UpdateBuilder()
                    .setAttributes(new AttributesBuilder(ab)
                            .addAugmentation(Attributes2.class, new Attributes2Builder()
                                    .setMpUnreachNlri(new MpUnreachNlriBuilder()
                                            .setAfi(Ipv6AddressFamily.class)
                                            .setSafi(UnicastSubsequentAddressFamily.class)
                                            .setWithdrawnRoutes(null)
                                            .build()).build()).build()).build();
            waitFutureSuccess(this.session.writeAndFlush(ud));
        }
    }

    static BgpParameters createParameter(final boolean addGraceful, final boolean afiFlags) {
        final OptionalCapabilities mp4 = new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().addAugmentation(
                        CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
                                new MultiprotocolCapabilityBuilder()
                                        .setAfi(Ipv4AddressFamily.class)
                                        .setSafi(UnicastSubsequentAddressFamily.class)
                                        .build()).build()).build()).build();
        final OptionalCapabilities mp6 = new OptionalCapabilitiesBuilder().setCParameters(
                new CParametersBuilder().addAugmentation(
                        CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
                                new MultiprotocolCapabilityBuilder()
                                        .setAfi(Ipv6AddressFamily.class)
                                        .setSafi(UnicastSubsequentAddressFamily.class)
                                        .build()).build()).build()).build();
        final List<OptionalCapabilities> capabilities = Lists.newArrayList(mp4, mp6);
        if (addGraceful) {
            final OptionalCapabilities gracefulCapability = new OptionalCapabilitiesBuilder().setCParameters(
                    new CParametersBuilder().addAugmentation(
                            CParameters1.class, new CParameters1Builder()
                                    .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                                            .setRestartFlags(new GracefulRestartCapability.RestartFlags(false))
                                            .setRestartTime(RESTART_TIME)
                                            .setTables(Collections.singletonList(new TablesBuilder()
                                                    .setAfi(Ipv4AddressFamily.class)
                                                    .setSafi(UnicastSubsequentAddressFamily.class)
                                                    .setAfiFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables.AfiFlags(afiFlags))
                                                    .build())).build()).build()).build()).build();
            capabilities.add(gracefulCapability);
        }
        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }

    private Open createClassicOpen(boolean addGraceful) {
        return new OpenBuilder()
                .setMyAsNumber((int) AS)
                .setHoldTimer(HOLDTIMER)
                .setVersion(new ProtocolVersion((short) 4))
                .setBgpParameters(Collections.singletonList(createParameter(addGraceful, true)))
                .setBgpIdentifier(PEER1)
                .build();
    }
}
