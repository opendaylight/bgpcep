/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.*;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.*;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborErrorHandlingStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.PeerGroupStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.SentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class StateProviderImplTest extends AbstractDataBrokerTest {
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final PortNumber localPort = new PortNumber(1790);
    private final PortNumber remotePort = new PortNumber(179);
    private final int restartTime = 15;
    private final String ribId = "identifier-test";
    private final InstanceIdentifier<Bgp> bgpInstanceIdentifier = InstanceIdentifier.create(NetworkInstances.class)
        .child(NetworkInstance.class, new NetworkInstanceKey("global-bgp")).child(Protocols.class)
        .child(Protocol.class, new ProtocolKey(BGP.class, this.ribId)).augmentation(Protocol1.class).child(Bgp.class);
    final static TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final AsNumber as = new AsNumber(72L);
    private final BgpId bgpId = new BgpId("127.0.0.1");
    private final IpAddress neighborAddress = new IpAddress(new Ipv4Address("127.0.0.2"));
    private final List<Class<? extends BgpCapability>> supportedCap = Arrays.asList(ASN32.class, ROUTEREFRESH.class,
            MPBGP.class, ADDPATHS.class, GRACEFULRESTART.class);
    @Mock
    private BGPStateConsumer stateCollector;
    @Mock
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    private BGPRIBState bgpRibState;
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
    @Mock
    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    @Mock
    private ClusterSingletonServiceRegistration singletonServiceRegistration;

    private final List<BGPPeerState> bgpPeerStates = new ArrayList<>();
    private final List<BGPRIBState> bgpRibStates = new ArrayList<>();
    private ClusterSingletonService singletonService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Mockito.doAnswer(invocationOnMock -> {
            this.singletonService = (ClusterSingletonService) invocationOnMock.getArguments()[0];
            return this.singletonServiceRegistration;
        }).when(this.clusterSingletonServiceProvider).registerClusterSingletonService(any(ClusterSingletonService.class));

        Mockito.doAnswer(invocationOnMock -> {
            this.singletonService.closeServiceInstance();
            return null;
        }).when(this.singletonServiceRegistration).close();

        doReturn(Optional.of(IPV4UNICAST.class))
            .when(this.tableTypeRegistry).getAfiSafiType(eq(TABLES_KEY));

        doReturn(this.bgpRibStates).when(this.stateCollector).getRibStats();
        doReturn(this.bgpPeerStates).when(this.stateCollector).getPeerStats();

        final KeyedInstanceIdentifier<Rib, RibKey> iid = InstanceIdentifier.create(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId(this.ribId)));
        doReturn(iid).when(this.bgpRibState).getInstanceIdentifier();
        doReturn(this.as).when(this.bgpRibState).getAs();
        doReturn(this.bgpId).when(this.bgpRibState).getRouteId();

        Mockito.doAnswer(invocation -> this.totalPathsCounter.longValue()).when(this.bgpRibState).getTotalPathsCount();
        Mockito.doAnswer(invocation -> this.totalPrefixesCounter.longValue()).when(this.bgpRibState).getTotalPrefixesCount();
        Mockito.doAnswer(invocation -> this.totalPathsCounter.longValue()).when(this.bgpRibState).getPathCount(eq(TABLES_KEY));
        Mockito.doAnswer(invocation -> this.totalPrefixesCounter.longValue()).when(this.bgpRibState).getPrefixesCount(eq(TABLES_KEY));
        Mockito.doAnswer(invocation -> Collections.singletonMap(TABLES_KEY,
            this.totalPrefixesCounter.longValue())).when(this.bgpRibState).getPrefixesCount();
        Mockito.doAnswer(invocation -> Collections.singletonMap(TABLES_KEY,
            this.totalPathsCounter.longValue())).when(this.bgpRibState).getPathsCount();

        // Mock Peer
        doReturn("test-group").when(this.bgpPeerState).getGroupId();
        doReturn(iid).when(this.bgpPeerState).getInstanceIdentifier();
        Mockito.doAnswer(invocation -> this.totalPrefixesCounter.longValue()).when(this.bgpPeerState).getTotalPrefixes();
        Mockito.doAnswer(invocation -> this.totalPathsCounter.longValue()).when(this.bgpPeerState).getTotalPathsCount();
        doReturn(this.neighborAddress).when(this.bgpPeerState).getNeighborAddress();
        doReturn(this.bgpSessionState).when(this.bgpPeerState).getBGPSessionState();
        doReturn(this.bgpPeerMessagesState).when(this.bgpPeerState).getBGPPeerMessagesState();

        doReturn(1L).when(this.bgpPeerMessagesState).getNotificationMessagesReceivedCount();
        doReturn(1L).when(this.bgpPeerMessagesState).getNotificationMessagesSentCount();
        doReturn(1L).when(this.bgpPeerMessagesState).getUpdateMessagesReceivedCount();
        doReturn(1L).when(this.bgpPeerMessagesState).getUpdateMessagesSentCount();
        doReturn(State.UP).when(this.bgpSessionState).getSessionState();
        doReturn(true).when(this.bgpSessionState).isAddPathCapabilitySupported();
        doReturn(true).when(this.bgpSessionState).isAsn32CapabilitySupported();
        doReturn(true).when(this.bgpSessionState).isGracefulRestartCapabilitySupported();
        doReturn(true).when(this.bgpSessionState).isMultiProtocolCapabilitySupported();
        doReturn(true).when(this.bgpSessionState).isRouterRefreshCapabilitySupported();

        doReturn(this.timersState).when(this.bgpPeerState).getBGPTimersState();
        doReturn(10L).when(this.timersState).getNegotiatedHoldTime();
        doReturn(1L).when(this.timersState).getUpTime();

        doReturn(this.bgpTransportState).when(this.bgpPeerState).getBGPTransportState();
        doReturn(this.localPort).when(this.bgpTransportState).getLocalPort();
        doReturn(this.neighborAddress).when(this.bgpTransportState).getRemoteAddress();
        doReturn(this.remotePort).when(this.bgpTransportState).getRemotePort();

        doReturn(this.bgpErrorHandlingState).when(this.bgpPeerState).getBGPErrorHandlingState();
        doReturn(1L).when(this.bgpErrorHandlingState).getErroneousUpdateReceivedCount();

        doReturn(this.bgpGracelfulRestartState).when(this.bgpPeerState).getBGPGracelfulRestart();
        doReturn(true).when(this.bgpGracelfulRestartState).isGracefulRestartAdvertized(any());
        doReturn(true).when(this.bgpGracelfulRestartState).isGracefulRestartReceived(any());
        doReturn(true).when(this.bgpGracelfulRestartState).isLocalRestarting();
        doReturn(true).when(this.bgpGracelfulRestartState).isPeerRestarting();
        doReturn(this.restartTime).when(this.bgpGracelfulRestartState).getPeerRestartTime();

        doReturn(this.bgpAfiSafiState).when(this.bgpPeerState).getBGPAfiSafiState();
        doReturn(Collections.singleton(TABLES_KEY)).when(this.bgpAfiSafiState).getAfiSafisAdvertized();
        doReturn(Collections.singleton(TABLES_KEY)).when(this.bgpAfiSafiState).getAfiSafisReceived();
        doReturn(1L).when(this.bgpAfiSafiState).getPrefixesInstalledCount(any());
        doReturn(2L).when(this.bgpAfiSafiState).getPrefixesReceivedCount(any());
        doReturn(1L).when(this.bgpAfiSafiState).getPrefixesSentCount(any());
        doReturn(true).when(this.bgpAfiSafiState).isAfiSafiSupported(any());
        doReturn(true).when(this.bgpAfiSafiState).isGracefulRestartAdvertized(any());
        doReturn(true).when(this.bgpAfiSafiState).isGracefulRestartReceived(any());
    }

    @Test
    public void testStateProvider() throws Exception {
        final StateProviderImpl stateProvider = new StateProviderImpl(getDataBroker(), 1, this.tableTypeRegistry,
            this.stateCollector, "global-bgp", this.clusterSingletonServiceProvider);
        this.singletonService.instantiateServiceInstance();

        final Global globalExpected = buildGlobalExpected(0);
        this.bgpRibStates.add(this.bgpRibState);
        readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            Assert.assertEquals(globalExpected, global);
            return bgpRib;
        });

        this.totalPathsCounter.increment();
        this.totalPrefixesCounter.increment();

        final Global globalExpected2 = buildGlobalExpected(1);
        readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            Assert.assertEquals(globalExpected2, global);
            return bgpRib;
        });

        this.totalPathsCounter.decrement();
        this.totalPrefixesCounter.decrement();

        final Global globalExpected3 = buildGlobalExpected(0);
        readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            Assert.assertEquals(globalExpected3, global);
            Assert.assertNull(bgpRib.getNeighbors());
            Assert.assertNull(bgpRib.getPeerGroups());
            return bgpRib;
        });

        this.bgpPeerStates.add(this.bgpPeerState);
        final PeerGroup peerGroupExpected = buildGroupExpected();

        this.totalPathsCounter.increment();
        this.totalPrefixesCounter.increment();

        final AfiSafis expectedAfiSafis = buildAfiSafis();
        final ErrorHandling expectedErrorHandling = buildErrorHandling();
        final GracefulRestart expectedGracefulRestart = buildGracefulRestart();
        final Transport expectedTransport = buildTransport();
        final Timers expectedTimers = buildTimers();
        final BgpNeighborStateAugmentation expectedBgpNeighborState = buildBgpNeighborStateAugmentation();

        readData(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Neighbors neighbors = bgpRib.getNeighbors();
            Assert.assertNotNull(neighbors);
            Assert.assertEquals(peerGroupExpected, bgpRib.getPeerGroups().getPeerGroup().get(0));
            final Neighbor neighborResult = neighbors.getNeighbor().get(0);
            Assert.assertEquals(this.neighborAddress, neighborResult.getNeighborAddress());
            Assert.assertEquals(expectedAfiSafis, neighborResult.getAfiSafis());
            Assert.assertEquals(expectedErrorHandling, neighborResult.getErrorHandling());
            Assert.assertEquals(expectedGracefulRestart, neighborResult.getGracefulRestart());
            Assert.assertEquals(expectedTransport, neighborResult.getTransport());
            Assert.assertEquals(expectedTimers, neighborResult.getTimers());
            final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.State stateResult =
                    neighborResult.getState();
            Assert.assertEquals(expectedBgpNeighborState, stateResult.getAugmentation(BgpNeighborStateAugmentation.class));
            Assert.assertEquals(BgpNeighborState.SessionState.ESTABLISHED, stateResult
                    .getAugmentation(NeighborStateAugmentation.class).getSessionState());
            final List<Class<? extends BgpCapability>> supportedCapabilitiesResult = stateResult
                    .getAugmentation(NeighborStateAugmentation.class).getSupportedCapabilities();
            Assert.assertTrue(supportedCapabilitiesResult.containsAll(this.supportedCap));
            return bgpRib;
        });

        this.bgpRibStates.clear();
        checkNull(getDataBroker(), this.bgpInstanceIdentifier);

        stateProvider.close();
    }

    private BgpNeighborStateAugmentation buildBgpNeighborStateAugmentation() {
        final BgpNeighborStateAugmentation augmentation = new BgpNeighborStateAugmentationBuilder()
                .setMessages(new MessagesBuilder().setReceived(new ReceivedBuilder()
                        .setNOTIFICATION(BigInteger.ONE).setUPDATE(BigInteger.ONE).build())
                        .setSent(new SentBuilder().setNOTIFICATION(BigInteger.ONE).setUPDATE(BigInteger.ONE).build())
                        .build()).build();
        return augmentation;
    }

    private AfiSafis buildAfiSafis() {
        final NeighborAfiSafiStateAugmentationBuilder neighborAfiSafiStateAugmentation =
                new NeighborAfiSafiStateAugmentationBuilder().setActive(true).setPrefixes(
                        new PrefixesBuilder().setSent(1L).setReceived(2L).setInstalled(1L).build());
        final AfiSafi afiSafi = new AfiSafiBuilder()
                .setAfiSafiName(IPV4UNICAST.class)
                .setGracefulRestart(new GracefulRestartBuilder().setState(new StateBuilder().setEnabled(false)
                        .addAugmentation(NeighborAfiSafiGracefulRestartStateAugmentation.class,
                                new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
                                        .setAdvertised(true).setReceived(true).build())
                        .build()).build())
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                        .common.afi.safi.list.afi.safi.StateBuilder().setEnabled(false).addAugmentation(
                                NeighborAfiSafiStateAugmentation.class, neighborAfiSafiStateAugmentation.build())
                        .build())
                .build();

        return new AfiSafisBuilder().setAfiSafi(Collections.singletonList(afiSafi)).build();
    }

    private ErrorHandling buildErrorHandling() {
        final ErrorHandling errorHandling = new ErrorHandlingBuilder().setState(
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.error.handling.
                        StateBuilder().setTreatAsWithdraw(false)
                        .addAugmentation(NeighborErrorHandlingStateAugmentation.class,
                                new NeighborErrorHandlingStateAugmentationBuilder().setErroneousUpdateMessages(1L).build()).build())
                .build();
        return errorHandling;
    }

    private Timers buildTimers() {
        final Timers timers = new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.
                bgp.rev151009.bgp.neighbor.group.timers.StateBuilder()
                .setConnectRetry(BigDecimal.valueOf(30))
                .setHoldTime(BigDecimal.valueOf(90))
                .setKeepaliveInterval(BigDecimal.valueOf(30))
                .setMinimumAdvertisementInterval(BigDecimal.valueOf(30))
                .addAugmentation(NeighborTimersStateAugmentation.class, new NeighborTimersStateAugmentationBuilder()
                        .setNegotiatedHoldTime(BigDecimal.TEN).setUptime(new Timeticks(1L)).build())
                .build()).build();
        return timers;
    }

    private Transport buildTransport() {
        final Transport transport = new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig
                .net.yang.bgp.rev151009.bgp.neighbor.group.transport.StateBuilder()
                .setMtuDiscovery(false)
                .setPassiveMode(false)
                .addAugmentation(NeighborTransportStateAugmentation.class,
                        new NeighborTransportStateAugmentationBuilder().setLocalPort(this.localPort)
                                .setRemotePort(this.remotePort)
                                .setRemoteAddress(this.neighborAddress).build())
                .build()).build();
        return transport;
    }

    private GracefulRestart buildGracefulRestart() {
        final NeighborGracefulRestartStateAugmentationBuilder gracefulAugmentation
                = new NeighborGracefulRestartStateAugmentationBuilder();
        gracefulAugmentation.setPeerRestarting(false);
        gracefulAugmentation.setLocalRestarting(false);
        gracefulAugmentation.setPeerRestartTime(0);
        gracefulAugmentation.setLocalRestarting(true)
                .setPeerRestarting(true).setPeerRestartTime(this.restartTime);
        final GracefulRestart gracefulRestart = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .rev151009.bgp.graceful.restart.GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http
                .openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
                .addAugmentation(NeighborGracefulRestartStateAugmentation.class,
                        gracefulAugmentation.build()).build()).build();
        return gracefulRestart;
    }

    private static <T extends DataObject> void checkNull(final DataBroker dataBroker, final InstanceIdentifier<T> iid)
        throws ReadFailedException {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            try (final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
                final com.google.common.base.Optional<T> data = tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet();
                    try {
                        assertFalse(data.isPresent());
                        return;
                    } catch (final AssertionError e) {
                        lastError = e;
                        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                    }
            }
        }
        Assert.fail(lastError.getMessage());
        throw lastError;
    }

    private static <R, T extends DataObject> void readData(final DataBroker dataBroker, final InstanceIdentifier<T> iid,
        final Function<T, R> function)
        throws ReadFailedException {
        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            try (final ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
                final com.google.common.base.Optional<T> data = tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet();
                if (data.isPresent()) {
                    try {
                        function.apply(data.get());
                        return;
                    } catch (final AssertionError e) {
                        lastError = e;
                        Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
        Assert.fail(lastError.getMessage());
        throw lastError;
    }

    private Global buildGlobalExpected(final long PrefixesAndPaths) {
        return new GlobalBuilder()
            .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.
                StateBuilder().setRouterId(new Ipv4Address(this.bgpId.getValue())).setTotalPrefixes(PrefixesAndPaths)
                .setTotalPaths(PrefixesAndPaths).setAs(this.as).build())
            .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.
                AfiSafisBuilder().setAfiSafi(Collections.singletonList(new AfiSafiBuilder()
                .setAfiSafiName(IPV4UNICAST.class).setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                    .bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder().setEnabled(false)
                    .addAugmentation(GlobalAfiSafiStateAugmentation.class, new GlobalAfiSafiStateAugmentationBuilder()
                        .setTotalPaths(PrefixesAndPaths).setTotalPrefixes(PrefixesAndPaths).build()).build()).build()))
                .build()).build();
    }

    private PeerGroup buildGroupExpected() {
        return new PeerGroupBuilder().setPeerGroupName("test-group").setState(new org.opendaylight.yang.gen.v1.http
            .openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder()
            .setSendCommunity(CommunityType.NONE)
            .setRouteFlapDamping(false)
            .addAugmentation(PeerGroupStateAugmentation.class,
                new PeerGroupStateAugmentationBuilder().setTotalPaths(1L).setTotalPrefixes(1L)
                    .build()).build())
            .build();
    }
}