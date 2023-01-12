/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiator;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.GraphKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionErrorPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.NoOpObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public abstract class AbstractPCEPSessionTest extends AbstractConcurrentDataBrokerTest {

    static final short RPC_TIMEOUT = 4;
    private static final TopologyKey TEST_TOPOLOGY_ID = new TopologyKey(new TopologyId("testtopo"));
    static final KeyedInstanceIdentifier<Topology, TopologyKey> TOPO_IID =
        InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, TEST_TOPOLOGY_ID);
    private static final String IPV4_MASK = "/32";
    final String testAddress = InetSocketAddressUtil.getRandomLoopbackIpAddress();
    final NodeId nodeId = new NodeId("pcc://" + testAddress);
    protected final InstanceIdentifier<PathComputationClient> pathComputationClientIId = TOPO_IID.builder()
            .child(Node.class, new NodeKey(nodeId))
            .augmentation(Node1.class)
            .child(PathComputationClient.class)
            .build();
    final String eroIpPrefix = testAddress + IPV4_MASK;
    final String newDestinationAddress = InetSocketAddressUtil.getRandomLoopbackIpAddress();
    final String dstIpPrefix = newDestinationAddress + IPV4_MASK;
    private final Open localPrefs = new OpenBuilder()
        .setDeadTimer(Uint8.valueOf(30))
        .setKeepalive(Uint8.TEN)
        .setSessionId(Uint8.ZERO)
        .build();
    private final Open remotePrefs = localPrefs;

    List<Notification<?>> receivedMsgs;
    ServerSessionManager manager;
    NetworkTopologyPcepService topologyRpcs;
    @Mock
    private EventLoop eventLoop;
    @Mock
    private Channel clientListener;
    @Mock
    private ChannelPipeline pipeline;
    @Mock
    private ChannelFuture channelFuture;
    @Mock
    private SessionStateRegistry stateRegistry;
    @Mock
    private PCEPTopologyProviderDependencies topologyDependencies;
    @Mock
    private Promise<PCEPSessionImpl> promise;
    @Mock
    private PcepSessionErrorPolicy errorPolicy;

    private final Timer timer = new HashedWheelTimer();
    private DefaultPCEPSessionNegotiator neg;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        receivedMsgs = new ArrayList<>();
        doAnswer(invocation -> {
            receivedMsgs.add(invocation.getArgument(0, Notification.class));
            return channelFuture;
        }).when(clientListener).writeAndFlush(any(Notification.class));
        doReturn(null).when(channelFuture).addListener(any());
        doReturn("TestingChannel").when(clientListener).toString();
        doReturn(pipeline).when(clientListener).pipeline();
        doReturn(pipeline).when(pipeline).replace(any(ChannelHandler.class), any(String.class),
            any(ChannelHandler.class));
        doReturn(eventLoop).when(clientListener).eventLoop();
        doAnswer(inv -> NoOpObjectRegistration.of(inv.getArgument(0, SessionStateUpdater.class)))
            .when(stateRegistry).bind(any());
        doReturn(null).when(eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(true).when(clientListener).isActive();
        final InetSocketAddress ra = new InetSocketAddress(testAddress, 4189);
        doReturn(ra).when(clientListener).remoteAddress();
        final InetSocketAddress la = new InetSocketAddress(testAddress, InetSocketAddressUtil.getRandomPort());
        doReturn(la).when(clientListener).localAddress();

        doReturn(mock(ChannelFuture.class)).when(clientListener).close();

        doReturn(getDataBroker()).when(topologyDependencies).getDataBroker();
        doReturn(stateRegistry).when(topologyDependencies).getStateRegistry();
        doReturn(timer).when(topologyDependencies).getTimer();
        doReturn(null).when(topologyDependencies).getPceServerProvider();

        doReturn(Uint16.valueOf(5)).when(errorPolicy).getMaxUnknownMessages();

        manager = customizeSessionManager(new ServerSessionManager(TOPO_IID, topologyDependencies,
                new GraphKey("graph-test"), RPC_TIMEOUT, TimeUnit.SECONDS.toNanos(5)));
        startSessionManager();
        neg = new DefaultPCEPSessionNegotiator(promise, clientListener, manager.getSessionListener(), Uint8.ONE,
            localPrefs, errorPolicy);
        topologyRpcs = new TopologyRPCs(manager);
    }

    // Visible for TopologyProgrammingTest
    ServerSessionManager customizeSessionManager(final ServerSessionManager original) {
        return original;
    }

    void startSessionManager() throws Exception {
        assertTrue(manager.start().get());
        assertFalse(manager.isClosed());
    }

    void stopSessionManager() {
        manager.stop();
    }

    @After
    public void tearDown() {
        stopSessionManager();
        timer.stop();
    }

    Ero createEroWithIpPrefixes(final List<String> ipPrefixes) {
        final List<Subobject> subobjs = new ArrayList<>(ipPrefixes.size());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder();
        for (final String ipPrefix : ipPrefixes) {
            subobjBuilder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder()
                    .setIpPrefix(new IpPrefix(new Ipv4Prefix(ipPrefix))).build()).build());
            subobjs.add(subobjBuilder.build());
        }
        return new EroBuilder().setSubobject(subobjs).build();
    }

    String getLastEroIpPrefix(final Ero ero) {
        return ((IpPrefixCase) ero.getSubobject().get(ero.getSubobject().size() - 1).getSubobjectType()).getIpPrefix()
                .getIpPrefix().getIpv4Prefix().getValue();
    }

    protected Open getLocalPref() {
        return localPrefs;
    }

    protected Open getRemotePref() {
        return remotePrefs;
    }

    protected PCEPTopologySessionListener getSessionListener() {
        return manager.getSessionListener();
    }

    protected final PCEPSessionImpl getPCEPSession(final Open localOpen, final Open remoteOpen) {
        return neg.createSession(clientListener, localOpen, remoteOpen);
    }
}
