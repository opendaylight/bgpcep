/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentOperational;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.infrautils.testutils.LogCapture;
import org.opendaylight.infrautils.testutils.internal.RememberingLogger;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafis;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandling;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ErrorHandlingBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ADDPATHS;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ASN32;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCapability;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.CommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.GRACEFULRESTART;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.MPBGP;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ROUTEREFRESH;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.OpenconfigNetworkInstanceData;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborErrorHandlingStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.PeerGroupStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.SentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class StateProviderImplTest extends AbstractDataBrokerTest {
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final PortNumber localPort = new PortNumber(Uint16.valueOf(1790));
    private final PortNumber remotePort = new PortNumber(Uint16.valueOf(179));
    private final Uint16 restartTime = Uint16.valueOf(15);
    private final String ribId = "identifier-test";
    private final InstanceIdentifier<Bgp> bgpInstanceIdentifier =
        InstanceIdentifier.builderOfInherited(OpenconfigNetworkInstanceData.class, NetworkInstances.class).build()
            .child(NetworkInstance.class, new NetworkInstanceKey("global-bgp"))
            .child(Protocols.class)
            .child(Protocol.class, new ProtocolKey(BGP.class, ribId))
            .augmentation(NetworkInstanceProtocol.class)
            .child(Bgp.class);
    static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final AsNumber as = new AsNumber(Uint32.valueOf(72));
    private final BgpId bgpId = new BgpId("127.0.0.1");
    private final IpAddressNoZone neighborAddress = new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.2"));
    private final List<Class<? extends BgpCapability>> supportedCap = List.of(ASN32.class, ROUTEREFRESH.class,
            MPBGP.class, ADDPATHS.class, GRACEFULRESTART.class);
    @Mock
    private BGPStateProvider stateProvider;
    @Mock
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    private BGPRibState bgpRibState;
    @Mock
    private BGPPeerState bgpPeerState;
    @Mock
    private BGPSessionState bgpSessionState;
    @Mock
    private BGPPeerMessagesState bgpPeerMessagesState;
    @Mock
    private BGPTimersState timersState;
    @Mock
    private BGPTransportState bgpTransportState;
    @Mock
    private BGPErrorHandlingState bgpErrorHandlingState;
    @Mock
    private BGPGracelfulRestartState bgpGracelfulRestartState;
    @Mock
    private BGPAfiSafiState bgpAfiSafiState;

    private final List<BGPPeerState> bgpPeerStates = new ArrayList<>();
    private final List<BGPRibState> bgpRibStates = new ArrayList<>();

    private InMemoryDOMDataStore realOperStore;
    private InMemoryDOMDataStore spiedOperStore;

    @Before
    public void setUp() {
        doReturn(IPV4UNICAST.class).when(tableTypeRegistry).getAfiSafiType(eq(TABLES_KEY));

        doReturn(bgpRibStates).when(stateProvider).getRibStats();
        doReturn(bgpPeerStates).when(stateProvider).getPeerStats();

        final KeyedInstanceIdentifier<Rib, RibKey> iid = InstanceIdentifier.create(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId(ribId)));
        doReturn(iid).when(bgpRibState).getInstanceIdentifier();
        doReturn(as).when(bgpRibState).getAs();
        doReturn(bgpId).when(bgpRibState).getRouteId();

        doAnswer(invocation -> totalPathsCounter.longValue())
                .when(bgpRibState).getTotalPathsCount();
        doAnswer(invocation -> totalPrefixesCounter.longValue())
                .when(bgpRibState).getTotalPrefixesCount();
        doAnswer(invocation -> totalPathsCounter.longValue())
                .when(bgpRibState).getPathCount(eq(TABLES_KEY));
        doAnswer(invocation -> totalPrefixesCounter.longValue())
                .when(bgpRibState).getPrefixesCount(eq(TABLES_KEY));
        doAnswer(invocation -> Map.of(TABLES_KEY,
            totalPathsCounter.longValue())).when(bgpRibState).getPathsCount();

        // Mock Peer
        doReturn("test-group").when(bgpPeerState).getGroupId();
        doReturn(iid).when(bgpPeerState).getInstanceIdentifier();
        doAnswer(invocation -> totalPrefixesCounter.longValue()).when(bgpPeerState).getTotalPrefixes();
        doAnswer(invocation -> totalPathsCounter.longValue()).when(bgpPeerState).getTotalPathsCount();
        doReturn(neighborAddress).when(bgpPeerState).getNeighborAddress();
        doReturn(bgpSessionState).when(bgpPeerState).getBGPSessionState();
        doReturn(bgpPeerMessagesState).when(bgpPeerState).getBGPPeerMessagesState();

        doReturn(1L).when(bgpPeerMessagesState).getNotificationMessagesReceivedCount();
        doReturn(1L).when(bgpPeerMessagesState).getNotificationMessagesSentCount();
        doReturn(1L).when(bgpPeerMessagesState).getUpdateMessagesReceivedCount();
        doReturn(1L).when(bgpPeerMessagesState).getUpdateMessagesSentCount();
        doReturn(State.UP).when(bgpSessionState).getSessionState();
        doReturn(true).when(bgpSessionState).isAddPathCapabilitySupported();
        doReturn(true).when(bgpSessionState).isAsn32CapabilitySupported();
        doReturn(true).when(bgpSessionState).isGracefulRestartCapabilitySupported();
        doReturn(true).when(bgpSessionState).isMultiProtocolCapabilitySupported();
        doReturn(true).when(bgpSessionState).isRouterRefreshCapabilitySupported();

        doReturn(timersState).when(bgpPeerState).getBGPTimersState();
        doReturn(10L).when(timersState).getNegotiatedHoldTime();
        doReturn(10L).when(timersState).getUpTime();

        doReturn(bgpTransportState).when(bgpPeerState).getBGPTransportState();
        doReturn(localPort).when(bgpTransportState).getLocalPort();
        doReturn(neighborAddress).when(bgpTransportState).getRemoteAddress();
        doReturn(remotePort).when(bgpTransportState).getRemotePort();

        doReturn(bgpErrorHandlingState).when(bgpPeerState).getBGPErrorHandlingState();
        doReturn(1L).when(bgpErrorHandlingState).getErroneousUpdateReceivedCount();

        doReturn(bgpGracelfulRestartState).when(bgpPeerState).getBGPGracelfulRestart();
        doReturn(true).when(bgpGracelfulRestartState).isLocalRestarting();
        doReturn(true).when(bgpGracelfulRestartState).isPeerRestarting();
        doReturn(restartTime.toJava()).when(bgpGracelfulRestartState).getPeerRestartTime();
        doReturn(BgpAfiSafiGracefulRestartState.Mode.BILATERAL).when(bgpGracelfulRestartState).getMode();

        doReturn(bgpAfiSafiState).when(bgpPeerState).getBGPAfiSafiState();
        doReturn(Set.of(TABLES_KEY)).when(bgpAfiSafiState).getAfiSafisAdvertized();
        doReturn(Set.of(TABLES_KEY)).when(bgpAfiSafiState).getAfiSafisReceived();
        doReturn(1L).when(bgpAfiSafiState).getPrefixesInstalledCount(any());
        doReturn(2L).when(bgpAfiSafiState).getPrefixesReceivedCount(any());
        doReturn(1L).when(bgpAfiSafiState).getPrefixesSentCount(any());
        doReturn(true).when(bgpAfiSafiState).isAfiSafiSupported(any());
        doReturn(true).when(bgpAfiSafiState).isGracefulRestartAdvertized(any());
        doReturn(true).when(bgpAfiSafiState).isGracefulRestartReceived(any());
        doReturn(true).when(bgpAfiSafiState).isLlGracefulRestartAdvertised(any());
        doReturn(true).when(bgpAfiSafiState).isLlGracefulRestartReceived(any());
        doReturn(60).when(bgpAfiSafiState).getLlGracefulRestartTimer(any());
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new ConcurrentDataBrokerTestCustomizer(true) {
            @Override
            public DOMStore createOperationalDatastore() {
                realOperStore = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
                spiedOperStore = spy(realOperStore);
                getSchemaService().registerSchemaContextListener(spiedOperStore);
                return spiedOperStore;
            }

            @Override
            public ListeningExecutorService getCommitCoordinatorExecutor() {
                return MoreExecutors.newDirectExecutorService();
            }
        };
    }

    @Test
    public void testActiveStateProvider() throws Exception {
        doReturn(true).when(bgpRibState).isActive();
        doReturn(true).when(bgpPeerState).isActive();

        try (StateProviderImpl stateProvider =
                // FIXME: use a properly-controlled executor service
                new StateProviderImpl(getDataBroker(), 1, tableTypeRegistry, this.stateProvider, "global-bgp")) {

            final Global globalExpected = buildGlobalExpected(0);
            bgpRibStates.add(bgpRibState);
            readDataOperational(getDataBroker(), bgpInstanceIdentifier, bgpRib -> {
                final Global global = bgpRib.getGlobal();
                assertEquals(globalExpected, global);
                return bgpRib;
            });

            totalPathsCounter.increment();
            totalPrefixesCounter.increment();

            final Global globalExpected2 = buildGlobalExpected(1);
            readDataOperational(getDataBroker(), bgpInstanceIdentifier, bgpRib -> {
                final Global global = bgpRib.getGlobal();
                assertEquals(globalExpected2, global);
                return bgpRib;
            });

            totalPathsCounter.decrement();
            totalPrefixesCounter.decrement();

            final Global globalExpected3 = buildGlobalExpected(0);
            readDataOperational(getDataBroker(), bgpInstanceIdentifier, bgpRib -> {
                final Global global = bgpRib.getGlobal();
                assertEquals(globalExpected3, global);
                assertNull(bgpRib.getNeighbors());
                assertNull(bgpRib.getPeerGroups());
                return bgpRib;
            });

            bgpPeerStates.add(bgpPeerState);
            final PeerGroup peerGroupExpected = buildGroupExpected();

            totalPathsCounter.increment();
            totalPrefixesCounter.increment();

            final AfiSafis expectedAfiSafis = buildAfiSafis();
            final ErrorHandling expectedErrorHandling = buildErrorHandling();
            final GracefulRestart expectedGracefulRestart = buildGracefulRestart();
            final Transport expectedTransport = buildTransport();
            final Timers expectedTimers = buildTimers();
            final BgpNeighborStateAugmentation expectedBgpNeighborState = buildBgpNeighborStateAugmentation();

            readDataOperational(getDataBroker(), bgpInstanceIdentifier, bgpRib -> {
                final Neighbors neighbors = bgpRib.getNeighbors();
                assertNotNull(neighbors);
                assertEquals(peerGroupExpected, bgpRib.getPeerGroups().nonnullPeerGroup().values().iterator().next());
                final Neighbor neighborResult = neighbors.nonnullNeighbor().values().iterator().next();
                assertEquals(new IpAddress(neighborAddress.getIpv4AddressNoZone()),
                    neighborResult.getNeighborAddress());
                assertEquals(expectedAfiSafis, neighborResult.getAfiSafis());
                assertEquals(expectedErrorHandling, neighborResult.getErrorHandling());
                assertEquals(expectedGracefulRestart, neighborResult.getGracefulRestart());
                assertEquals(expectedTransport, neighborResult.getTransport());
                assertEquals(expectedTimers, neighborResult.getTimers());
                final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .State stateResult = neighborResult.getState();
                assertEquals(expectedBgpNeighborState, stateResult.augmentation(BgpNeighborStateAugmentation.class));
                assertEquals(BgpNeighborState.SessionState.ESTABLISHED, stateResult
                    .augmentation(NeighborStateAugmentation.class).getSessionState());
                final Set<Class<? extends BgpCapability>> supportedCapabilitiesResult = stateResult
                    .augmentation(NeighborStateAugmentation.class).getSupportedCapabilities();
                assertTrue(supportedCapabilitiesResult.containsAll(supportedCap));
                return bgpRib;
            });

            bgpRibStates.clear();
            checkNotPresentOperational(getDataBroker(), bgpInstanceIdentifier);
        }
    }

    @Test
    public void testInactiveStateProvider() throws Exception {
        doReturn(false).when(bgpRibState).isActive();

        try (StateProviderImpl stateProvider =
                new StateProviderImpl(getDataBroker(), 100, TimeUnit.MILLISECONDS, tableTypeRegistry,
                        this.stateProvider,
                    // FIXME: use a properly-controlled executor service ...
                    "global-bgp", Executors.newScheduledThreadPool(1))) {

            bgpRibStates.add(bgpRibState);
            /// ... and trigger here
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            checkNotPresentOperational(getDataBroker(), bgpInstanceIdentifier);

            bgpPeerStates.add(bgpPeerState);
            /// ... and trigger here
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            checkNotPresentOperational(getDataBroker(), bgpInstanceIdentifier);

            bgpRibStates.clear();
            /// ... and trigger here
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            checkNotPresentOperational(getDataBroker(), bgpInstanceIdentifier);
        }
    }

    @Test
    public void testTransactionChainFailure() throws Exception {
        if (!(LoggerFactory.getLogger(StateProviderImpl.class) instanceof RememberingLogger)) {
            throw new IllegalStateException("infrautils-testutils must be on the classpath BEFORE other logger impls"
                + LoggerFactory.getLogger(StateProviderImpl.class).getClass());
        }

        doReturn(true).when(bgpRibState).isActive();

        bgpRibStates.add(bgpRibState);

        ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
        doReturn(true).when(mockScheduledFuture).cancel(anyBoolean());

        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        doReturn(mockScheduledFuture).when(mockScheduler).scheduleAtFixedRate(any(Runnable.class), anyLong(),
                anyLong(), any(TimeUnit.class));
        doNothing().when(mockScheduler).shutdown();

        DOMStoreTransactionChain mockTxChain = mock(DOMStoreTransactionChain.class);

        Throwable mockCommitEx = new Exception("mock commit failure");
        doAnswer(invocation -> {
            DOMStoreThreePhaseCommitCohort mockCohort = mock(DOMStoreThreePhaseCommitCohort.class);
            doReturn(Futures.immediateFailedFuture(mockCommitEx)).when(mockCohort).canCommit();
            doReturn(Futures.immediateFuture(null)).when(mockCohort).abort();

            doAnswer(notused -> {
                DOMStoreWriteTransaction mockWriteTx = mock(DOMStoreReadWriteTransaction .class);
                doNothing().when(mockWriteTx).write(any(), any());
                doNothing().when(mockWriteTx).merge(any(), any());
                doReturn(mockCohort).when(mockWriteTx).ready();
                return mockWriteTx;
            }).when(mockTxChain).newReadWriteTransaction();

            return mockTxChain;
        }).doAnswer(invocation -> realOperStore.createTransactionChain()).when(spiedOperStore).createTransactionChain();

        final int period = 100;
        final TimeUnit unit = TimeUnit.MILLISECONDS;
        try (StateProviderImpl stateProvider = new StateProviderImpl(getDataBroker(), period, unit, tableTypeRegistry,
                this.stateProvider, "global-bgp", mockScheduler)) {

            ArgumentCaptor<Runnable> timerTask = ArgumentCaptor.forClass(Runnable.class);
            verify(mockScheduler).scheduleAtFixedRate(timerTask.capture(), eq(0L), eq((long)period), eq(unit));

            timerTask.getValue().run();

            String lastError = RememberingLogger.getLastErrorThrowable().orElseThrow(
                () -> new AssertionError("Expected logged ERROR")).toString();
            assertTrue("Last logged ERROR didn't contain expected string: " + lastError,
                    lastError.contains(mockCommitEx.getMessage()));

            RememberingLogger.resetLastError();

            timerTask.getValue().run();

            List<LogCapture> loggedErrors = RememberingLogger.getErrorLogCaptures();
            assertTrue("Expected no logged ERRORs: " + loggedErrors, loggedErrors.isEmpty());

            verify(spiedOperStore, times(2)).createTransactionChain();
        }
    }

    private static BgpNeighborStateAugmentation buildBgpNeighborStateAugmentation() {
        final BgpNeighborStateAugmentation augmentation = new BgpNeighborStateAugmentationBuilder()
                .setMessages(new MessagesBuilder().setReceived(new ReceivedBuilder()
                        .setNOTIFICATION(Uint64.ONE).setUPDATE(Uint64.ONE).build())
                        .setSent(new SentBuilder().setNOTIFICATION(Uint64.ONE).setUPDATE(Uint64.ONE).build())
                        .build()).build();
        return augmentation;
    }

    private static AfiSafis buildAfiSafis() {
        final NeighborAfiSafiStateAugmentationBuilder neighborAfiSafiStateAugmentation =
                new NeighborAfiSafiStateAugmentationBuilder()
                .setActive(true)
                .setPrefixes(new PrefixesBuilder()
                    .setSent(Uint32.ONE)
                    .setReceived(Uint32.TWO)
                    .setInstalled(Uint32.ONE)
                    .build());
        final AfiSafi afiSafi = new AfiSafiBuilder()
                .setAfiSafiName(IPV4UNICAST.class)
                .setGracefulRestart(new GracefulRestartBuilder()
                    .setState(new StateBuilder().setEnabled(false)
                        .addAugmentation(new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
                            .setAdvertised(true)
                            .setReceived(true)
                            .setLlStaleTimer(Uint32.valueOf(60))
                            .setLlAdvertised(true)
                            .setLlReceived(true)
                            .build())
                        .build())
                    .build())
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                        .common.afi.safi.list.afi.safi.StateBuilder()
                            .setEnabled(false)
                            .addAugmentation(neighborAfiSafiStateAugmentation.build())
                        .build())
                .build();

        return new AfiSafisBuilder().setAfiSafi(Map.of(afiSafi.key(), afiSafi)).build();
    }

    private static ErrorHandling buildErrorHandling() {
        final ErrorHandling errorHandling = new ErrorHandlingBuilder().setState(
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.error
                        .handling.StateBuilder().setTreatAsWithdraw(false)
                        .addAugmentation(new NeighborErrorHandlingStateAugmentationBuilder()
                                        .setErroneousUpdateMessages(Uint32.ONE).build()).build()).build();
        return errorHandling;
    }

    private static Timers buildTimers() {
        return new TimersBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .timers.StateBuilder()
                        .setConnectRetry(Decimal64.valueOf(2, 30))
                        .setHoldTime(Decimal64.valueOf(2, 90))
                        .setKeepaliveInterval(Decimal64.valueOf(2, 30))
                        .setMinimumAdvertisementInterval(Decimal64.valueOf(2, 30))
                        .addAugmentation(new NeighborTimersStateAugmentationBuilder()
                            .setNegotiatedHoldTime(Decimal64.valueOf(2, 10))
                            .setUptime(new Timeticks(Uint32.ONE)).build())
                        .build())
                .build();
    }

    private Transport buildTransport() {
        return new TransportBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig
                .net.yang.bgp.rev151009.bgp.neighbor.group.transport.StateBuilder()
                    .setMtuDiscovery(false)
                .setPassiveMode(false)
                .addAugmentation(new NeighborTransportStateAugmentationBuilder()
                    .setLocalPort(localPort)
                                .setRemotePort(remotePort)
                                .setRemoteAddress(new IpAddress(neighborAddress.getIpv4AddressNoZone())).build())
                .build()).build();
    }

    private GracefulRestart buildGracefulRestart() {
        final NeighborGracefulRestartStateAugmentationBuilder gracefulAugmentation
                = new NeighborGracefulRestartStateAugmentationBuilder()
                .setPeerRestarting(false)
                .setLocalRestarting(false)
                .setPeerRestartTime(Uint16.ZERO)
                .setLocalRestarting(true)
                .setPeerRestarting(true)
                .setPeerRestartTime(restartTime)
                .setMode(BgpAfiSafiGracefulRestartState.Mode.BILATERAL);
        final GracefulRestart gracefulRestart = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .rev151009.bgp.graceful.restart.GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http
                .openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
                .addAugmentation(gracefulAugmentation.build()).build()).build();
        return gracefulRestart;
    }

    private Global buildGlobalExpected(final long prefixesAndPaths) {
        return new GlobalBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                        .StateBuilder()
                            .setRouterId(new Ipv4Address(bgpId.getValue()))
                            .setTotalPrefixes(Uint32.valueOf(prefixesAndPaths))
                            .setTotalPaths(Uint32.valueOf(prefixesAndPaths))
                            .setAs(as)
                            .build())
                .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                        .AfiSafisBuilder()
                            .setAfiSafi(BindingMap.of(new AfiSafiBuilder()
                                .setAfiSafiName(IPV4UNICAST.class)
                                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol
                                    .rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder()
                                        .setEnabled(false)
                                        .addAugmentation(new GlobalAfiSafiStateAugmentationBuilder()
                                            .setTotalPaths(Uint32.valueOf(prefixesAndPaths))
                                            .setTotalPrefixes(Uint32.valueOf(prefixesAndPaths))
                                            .build())
                                        .build())
                                .build()))
                        .build())
                .build();
    }

    private static PeerGroup buildGroupExpected() {
        return new PeerGroupBuilder()
                .setPeerGroupName("test-group")
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .StateBuilder()
                    .setSendCommunity(CommunityType.NONE)
                    .setRouteFlapDamping(false)
                    .addAugmentation(new PeerGroupStateAugmentationBuilder()
                        .setTotalPaths(Uint32.ONE)
                        .setTotalPrefixes(Uint32.ONE)
                        .build())
                    .build())
                .build();
    }
}
