/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.state;

import static org.junit.Assert.assertEquals;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.infrautils.testutils.LogCapture;
import org.opendaylight.infrautils.testutils.internal.RememberingLogger;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
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
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.GlobalAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAfiSafiGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAfiSafiGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborErrorHandlingStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborGracefulRestartStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTransportStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.NetworkInstanceProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.PeerGroupStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.PeerGroupStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.SentBuilder;
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
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.LoggerFactory;

public class StateProviderImplTest extends AbstractDataBrokerTest {
    private final LongAdder totalPathsCounter = new LongAdder();
    private final LongAdder totalPrefixesCounter = new LongAdder();
    private final PortNumber localPort = new PortNumber(Uint16.valueOf(1790));
    private final PortNumber remotePort = new PortNumber(Uint16.valueOf(179));
    private final Uint16 restartTime = Uint16.valueOf(15);
    private final String ribId = "identifier-test";
    private final InstanceIdentifier<Bgp> bgpInstanceIdentifier = InstanceIdentifier.create(NetworkInstances.class)
        .child(NetworkInstance.class, new NetworkInstanceKey("global-bgp")).child(Protocols.class)
        .child(Protocol.class, new ProtocolKey(BGP.class, this.ribId)).augmentation(NetworkInstanceProtocol.class)
            .child(Bgp.class);
    static final TablesKey TABLES_KEY = new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
    private final AsNumber as = new AsNumber(Uint32.valueOf(72));
    private final BgpId bgpId = new BgpId("127.0.0.1");
    private final IpAddressNoZone neighborAddress = new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.2"));
    private final List<Class<? extends BgpCapability>> supportedCap = Arrays.asList(ASN32.class, ROUTEREFRESH.class,
            MPBGP.class, ADDPATHS.class, GRACEFULRESTART.class);
    @Mock
    private BGPStateConsumer stateCollector;
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
        MockitoAnnotations.initMocks(this);

        doReturn(Optional.of(IPV4UNICAST.class))
            .when(this.tableTypeRegistry).getAfiSafiType(eq(TABLES_KEY));

        doReturn(this.bgpRibStates).when(this.stateCollector).getRibStats();
        doReturn(this.bgpPeerStates).when(this.stateCollector).getPeerStats();

        final KeyedInstanceIdentifier<Rib, RibKey> iid = InstanceIdentifier.create(BgpRib.class)
            .child(Rib.class, new RibKey(new RibId(this.ribId)));
        doReturn(iid).when(this.bgpRibState).getInstanceIdentifier();
        doReturn(this.as).when(this.bgpRibState).getAs();
        doReturn(this.bgpId).when(this.bgpRibState).getRouteId();

        doAnswer(invocation -> this.totalPathsCounter.longValue())
                .when(this.bgpRibState).getTotalPathsCount();
        doAnswer(invocation -> this.totalPrefixesCounter.longValue())
                .when(this.bgpRibState).getTotalPrefixesCount();
        doAnswer(invocation -> this.totalPathsCounter.longValue())
                .when(this.bgpRibState).getPathCount(eq(TABLES_KEY));
        doAnswer(invocation -> this.totalPrefixesCounter.longValue())
                .when(this.bgpRibState).getPrefixesCount(eq(TABLES_KEY));
        doAnswer(invocation -> Collections.singletonMap(TABLES_KEY,
            this.totalPrefixesCounter.longValue())).when(this.bgpRibState).getTablesPrefixesCount();
        doAnswer(invocation -> Collections.singletonMap(TABLES_KEY,
            this.totalPathsCounter.longValue())).when(this.bgpRibState).getPathsCount();

        // Mock Peer
        doReturn("test-group").when(this.bgpPeerState).getGroupId();
        doReturn(iid).when(this.bgpPeerState).getInstanceIdentifier();
        doAnswer(invocation -> this.totalPrefixesCounter.longValue()).when(this.bgpPeerState).getTotalPrefixes();
        doAnswer(invocation -> this.totalPathsCounter.longValue()).when(this.bgpPeerState).getTotalPathsCount();
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
        doReturn(10L).when(this.timersState).getUpTime();

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
        doReturn(this.restartTime.toJava()).when(this.bgpGracelfulRestartState).getPeerRestartTime();
        doReturn(BgpAfiSafiGracefulRestartState.Mode.BILATERAL).when(this.bgpGracelfulRestartState).getMode();

        doReturn(this.bgpAfiSafiState).when(this.bgpPeerState).getBGPAfiSafiState();
        doReturn(Collections.singleton(TABLES_KEY)).when(this.bgpAfiSafiState).getAfiSafisAdvertized();
        doReturn(Collections.singleton(TABLES_KEY)).when(this.bgpAfiSafiState).getAfiSafisReceived();
        doReturn(1L).when(this.bgpAfiSafiState).getPrefixesInstalledCount(any());
        doReturn(2L).when(this.bgpAfiSafiState).getPrefixesReceivedCount(any());
        doReturn(1L).when(this.bgpAfiSafiState).getPrefixesSentCount(any());
        doReturn(true).when(this.bgpAfiSafiState).isAfiSafiSupported(any());
        doReturn(true).when(this.bgpAfiSafiState).isGracefulRestartAdvertized(any());
        doReturn(true).when(this.bgpAfiSafiState).isGracefulRestartReceived(any());
        doReturn(true).when(this.bgpAfiSafiState).isLlGracefulRestartAdvertised(any());
        doReturn(true).when(this.bgpAfiSafiState).isLlGracefulRestartReceived(any());
        doReturn(60).when(this.bgpAfiSafiState).getLlGracefulRestartTimer(any());
    }

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(NetworkInstances.class),
                BindingReflections.getModuleInfo(NetworkInstanceProtocol.class));
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
        doReturn(true).when(this.bgpRibState).isActive();
        doReturn(true).when(this.bgpPeerState).isActive();

        final StateProviderImpl stateProvider = new StateProviderImpl(getDataBroker(), 1, this.tableTypeRegistry,
            this.stateCollector, "global-bgp");
        stateProvider.init();

        final Global globalExpected = buildGlobalExpected(0);
        this.bgpRibStates.add(this.bgpRibState);
        readDataOperational(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            assertEquals(globalExpected, global);
            return bgpRib;
        });

        this.totalPathsCounter.increment();
        this.totalPrefixesCounter.increment();

        final Global globalExpected2 = buildGlobalExpected(1);
        readDataOperational(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            assertEquals(globalExpected2, global);
            return bgpRib;
        });

        this.totalPathsCounter.decrement();
        this.totalPrefixesCounter.decrement();

        final Global globalExpected3 = buildGlobalExpected(0);
        readDataOperational(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Global global = bgpRib.getGlobal();
            assertEquals(globalExpected3, global);
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

        readDataOperational(getDataBroker(), this.bgpInstanceIdentifier, bgpRib -> {
            final Neighbors neighbors = bgpRib.getNeighbors();
            Assert.assertNotNull(neighbors);
            assertEquals(peerGroupExpected, bgpRib.getPeerGroups().nonnullPeerGroup().values().iterator().next());
            final Neighbor neighborResult = neighbors.nonnullNeighbor().values().iterator().next();
            assertEquals(new IpAddress(neighborAddress.getIpv4AddressNoZone()), neighborResult.getNeighborAddress());
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
            final List<Class<? extends BgpCapability>> supportedCapabilitiesResult = stateResult
                    .augmentation(NeighborStateAugmentation.class).getSupportedCapabilities();
            Assert.assertTrue(supportedCapabilitiesResult.containsAll(this.supportedCap));
            return bgpRib;
        });

        this.bgpRibStates.clear();
        checkNotPresentOperational(getDataBroker(), this.bgpInstanceIdentifier);

        stateProvider.close();
    }

    @Test
    public void testInactiveStateProvider() throws Exception {
        doReturn(false).when(this.bgpRibState).isActive();
        doReturn(false).when(this.bgpPeerState).isActive();

        final StateProviderImpl stateProvider = new StateProviderImpl(getDataBroker(), 1, this.tableTypeRegistry,
            this.stateCollector, "global-bgp");
        stateProvider.init();

        this.bgpRibStates.add(this.bgpRibState);
        checkNotPresentOperational(getDataBroker(), this.bgpInstanceIdentifier);

        this.bgpPeerStates.add(this.bgpPeerState);
        checkNotPresentOperational(getDataBroker(), this.bgpInstanceIdentifier);

        this.bgpRibStates.clear();
        checkNotPresentOperational(getDataBroker(), this.bgpInstanceIdentifier);

        stateProvider.close();
    }

    @Test
    public void testTransactionChainFailure() throws Exception {
        if (!(LoggerFactory.getLogger(StateProviderImpl.class) instanceof RememberingLogger)) {
            throw new IllegalStateException("infrautils-testutils must be on the classpath BEFORE other logger impls"
                + LoggerFactory.getLogger(StateProviderImpl.class).getClass());
        }

        doReturn(true).when(this.bgpRibState).isActive();

        this.bgpRibStates.add(this.bgpRibState);

        ScheduledFuture<?> mockScheduledFuture = mock(ScheduledFuture.class);
        doReturn(true).when(mockScheduledFuture).cancel(anyBoolean());

        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        doReturn(mockScheduledFuture).when(mockScheduler).scheduleAtFixedRate(any(Runnable.class), anyLong(),
                anyLong(), any(TimeUnit.class));
        doNothing().when(mockScheduler).shutdown();

        DOMStoreTransactionChain mockTxChain = mock(DOMStoreTransactionChain.class);
        doNothing().when(mockTxChain).close();

        Throwable mockCommitEx = new Exception("mock commit failure");
        doAnswer(invocation -> {
            DOMStoreThreePhaseCommitCohort mockCohort = mock(DOMStoreThreePhaseCommitCohort.class);
            doReturn(Futures.immediateFailedFuture(mockCommitEx)).when(mockCohort).canCommit();
            doReturn(Futures.immediateFuture(null)).when(mockCohort).abort();

            doAnswer(notused -> {
                DOMStoreWriteTransaction mockWriteTx = mock(DOMStoreReadWriteTransaction .class);
                doNothing().when(mockWriteTx).write(any(), any());
                doNothing().when(mockWriteTx).merge(any(), any());
                doNothing().when(mockWriteTx).delete(any());
                doReturn(mockCohort).when(mockWriteTx).ready();
                return mockWriteTx;
            }).when(mockTxChain).newReadWriteTransaction();

            return mockTxChain;
        }).doAnswer(invocation -> realOperStore.createTransactionChain()).when(spiedOperStore).createTransactionChain();

        int timerInterval = 1;
        try (StateProviderImpl stateProvider = new StateProviderImpl(getDataBroker(), timerInterval, tableTypeRegistry,
                stateCollector, "global-bgp", mockScheduler)) {
            stateProvider.init();

            ArgumentCaptor<Runnable> timerTask = ArgumentCaptor.forClass(Runnable.class);
            verify(mockScheduler).scheduleAtFixedRate(timerTask.capture(), eq(0L), eq((long)timerInterval),
                    eq(TimeUnit.SECONDS));

            timerTask.getValue().run();

            String lastError = RememberingLogger.getLastErrorThrowable().orElseThrow(
                () -> new AssertionError("Expected logged ERROR")).toString();
            assertTrue("Last logged ERROR didn't contain expected string: " + lastError,
                    lastError.contains(mockCommitEx.getMessage()));

            RememberingLogger.resetLastError();

            timerTask.getValue().run();

            ImmutableList<LogCapture> loggedErrors = RememberingLogger.getErrorLogCaptures();
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
                .setGracefulRestart(new GracefulRestartBuilder().setState(new StateBuilder().setEnabled(false)
                        .addAugmentation(NeighborAfiSafiGracefulRestartStateAugmentation.class,
                                new NeighborAfiSafiGracefulRestartStateAugmentationBuilder()
                                        .setAdvertised(true)
                                        .setReceived(true)
                                        .setLlStaleTimer(Uint32.valueOf(60))
                                        .setLlAdvertised(true)
                                        .setLlReceived(true)
                                        .build())
                        .build()).build())
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                        .common.afi.safi.list.afi.safi.StateBuilder().setEnabled(false).addAugmentation(
                                NeighborAfiSafiStateAugmentation.class, neighborAfiSafiStateAugmentation.build())
                        .build())
                .build();

        return new AfiSafisBuilder().setAfiSafi(Collections.singletonList(afiSafi)).build();
    }

    private static ErrorHandling buildErrorHandling() {
        final ErrorHandling errorHandling = new ErrorHandlingBuilder().setState(
                new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.error
                        .handling.StateBuilder().setTreatAsWithdraw(false)
                        .addAugmentation(NeighborErrorHandlingStateAugmentation.class,
                                new NeighborErrorHandlingStateAugmentationBuilder()
                                        .setErroneousUpdateMessages(Uint32.ONE).build()).build()).build();
        return errorHandling;
    }

    private static Timers buildTimers() {
        final Timers timers = new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                .bgp.rev151009.bgp.neighbor.group.timers.StateBuilder()
                .setConnectRetry(BigDecimal.valueOf(30))
                .setHoldTime(BigDecimal.valueOf(90))
                .setKeepaliveInterval(BigDecimal.valueOf(30))
                .setMinimumAdvertisementInterval(BigDecimal.valueOf(30))
                .addAugmentation(NeighborTimersStateAugmentation.class, new NeighborTimersStateAugmentationBuilder()
                        .setNegotiatedHoldTime(BigDecimal.TEN).setUptime(new Timeticks(Uint32.ONE)).build())
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
                                .setRemoteAddress(new IpAddress(neighborAddress.getIpv4AddressNoZone())).build())
                .build()).build();
        return transport;
    }

    private GracefulRestart buildGracefulRestart() {
        final NeighborGracefulRestartStateAugmentationBuilder gracefulAugmentation
                = new NeighborGracefulRestartStateAugmentationBuilder()
                .setPeerRestarting(false)
                .setLocalRestarting(false)
                .setPeerRestartTime(Uint16.ZERO)
                .setLocalRestarting(true)
                .setPeerRestarting(true)
                .setPeerRestartTime(this.restartTime)
                .setMode(BgpAfiSafiGracefulRestartState.Mode.BILATERAL);
        final GracefulRestart gracefulRestart = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .rev151009.bgp.graceful.restart.GracefulRestartBuilder().setState(new org.opendaylight.yang.gen.v1.http
                .openconfig.net.yang.bgp.rev151009.bgp.graceful.restart.graceful.restart.StateBuilder()
                .addAugmentation(NeighborGracefulRestartStateAugmentation.class,
                        gracefulAugmentation.build()).build()).build();
        return gracefulRestart;
    }

    private Global buildGlobalExpected(final long prefixesAndPaths) {
        return new GlobalBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                        .StateBuilder()
                            .setRouterId(new Ipv4Address(this.bgpId.getValue()))
                            .setTotalPrefixes(Uint32.valueOf(prefixesAndPaths))
                            .setTotalPaths(Uint32.valueOf(prefixesAndPaths))
                            .setAs(this.as)
                            .build())
                .setAfiSafis(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base
                        .AfiSafisBuilder()
                            .setAfiSafi(Collections.singletonList(new AfiSafiBuilder()
                                .setAfiSafiName(IPV4UNICAST.class)
                                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol
                                    .rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder()
                                        .setEnabled(false)
                                        .addAugmentation(GlobalAfiSafiStateAugmentation.class,
                                            new GlobalAfiSafiStateAugmentationBuilder()
                                                .setTotalPaths(Uint32.valueOf(prefixesAndPaths))
                                                .setTotalPrefixes(Uint32.valueOf(prefixesAndPaths))
                                                .build())
                                        .build())
                                .build()))
                        .build())
                .build();
    }

    private static PeerGroup buildGroupExpected() {
        return new PeerGroupBuilder().setPeerGroupName("test-group").setState(new org.opendaylight.yang.gen.v1.http
            .openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder()
            .setSendCommunity(CommunityType.NONE)
            .setRouteFlapDamping(false)
            .addAugmentation(PeerGroupStateAugmentation.class, new PeerGroupStateAugmentationBuilder()
                .setTotalPaths(Uint32.ONE)
                .setTotalPrefixes(Uint32.ONE)
                .build()).build())
            .build();
    }
}
