/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyConfiguration;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderDependencies;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.SpeakerIdMapping;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiator;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.util.InetSocketAddressUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class AbstractPCEPSessionTest<T extends TopologySessionListenerFactory>
        extends AbstractConcurrentDataBrokerTest {

    static final short DEAD_TIMER = 30;
    static final short KEEP_ALIVE = 10;
    static final short RPC_TIMEOUT = 4;
    private static final TopologyId TEST_TOPOLOGY_ID = new TopologyId("testtopo");
    static final InstanceIdentifier<Topology> TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(TEST_TOPOLOGY_ID)).build();
    private static final String IPV4_MASK = "/32";
    final String testAddress = InetSocketAddressUtil.getRandomLoopbackIpAddress();
    final NodeId nodeId = new NodeId("pcc://" + this.testAddress);
    protected final InstanceIdentifier<PathComputationClient> pathComputationClientIId = TOPO_IID.builder()
            .child(Node.class, new NodeKey(this.nodeId)).augmentation(Node1.class).child(PathComputationClient.class
            ).build();
    final String eroIpPrefix = this.testAddress + IPV4_MASK;
    final String newDestinationAddress = InetSocketAddressUtil.getRandomLoopbackIpAddress();
    final String dstIpPrefix = this.newDestinationAddress + IPV4_MASK;
    private final Open localPrefs = new OpenBuilder().setDeadTimer((short) 30).setKeepalive((short) 10)
            .setSessionId((short) 0).build();
    private final Open remotePrefs = this.localPrefs;
    List<Notification> receivedMsgs;
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
    private TopologySessionStatsRegistry statsRegistry;
    @Mock
    private PCEPTopologyProviderDependencies topologyDependencies;
    @Mock
    private InstructionScheduler scheduler;
    @Mock
    private Promise<PCEPSessionImpl> promise;
    private DefaultPCEPSessionNegotiator neg;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.receivedMsgs = new ArrayList<>();
        doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            AbstractPCEPSessionTest.this.receivedMsgs.add((Notification) args[0]);
            return this.channelFuture;
        }).when(this.clientListener).writeAndFlush(any(Notification.class));
        doReturn(null).when(this.channelFuture).addListener(Mockito.any());
        doReturn("TestingChannel").when(this.clientListener).toString();
        doReturn(this.pipeline).when(this.clientListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class),
                any(ChannelHandler.class));
        doReturn(this.eventLoop).when(this.clientListener).eventLoop();
        doNothing().when(this.statsRegistry).bind(any(), any());
        doNothing().when(this.statsRegistry).unbind(any());
        doReturn(null).when(this.eventLoop).schedule(any(Runnable.class), any(long.class),
                any(TimeUnit.class));
        doReturn(true).when(this.clientListener).isActive();
        final InetSocketAddress ra = new InetSocketAddress(this.testAddress, 4189);
        doReturn(ra).when(this.clientListener).remoteAddress();
        final InetSocketAddress la = new InetSocketAddress(this.testAddress, InetSocketAddressUtil.getRandomPort());
        doReturn(la).when(this.clientListener).localAddress();

        doReturn(mock(ChannelFuture.class)).when(this.clientListener).close();

        doReturn(getDataBroker()).when(this.topologyDependencies).getDataBroker();
        doReturn(this.statsRegistry).when(this.topologyDependencies).getStateRegistry();

        @SuppressWarnings("unchecked") final T listenerFactory = (T) ((Class) ((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0]).newInstance();
        this.manager = new ServerSessionManager(this.topologyDependencies, TOPO_IID, listenerFactory,
                new PCEPTopologyConfiguration(ra,
                        KeyMapping.getKeyMapping(),
                        SpeakerIdMapping.getSpeakerIdMap(),
                        this.scheduler,
                        TEST_TOPOLOGY_ID,
                        RPC_TIMEOUT));
        startSessionManager();
        this.neg = new DefaultPCEPSessionNegotiator(this.promise, this.clientListener,
                this.manager.getSessionListener(), (short) 1, 5, this.localPrefs);
        this.topologyRpcs = new TopologyRPCs(this.manager);
    }

    void startSessionManager() throws Exception {
        final ListenableFuture<Void> future = this.manager.instantiateServiceInstance();
        future.get();
        checkEquals(() -> assertFalse(this.manager.isClosed.get()));
    }

    void stopSessionManager() {
        this.manager.closeServiceInstance();
    }

    @After
    public void tearDown() {
        stopSessionManager();
    }

    Ero createEroWithIpPrefixes(final List<String> ipPrefixes) {
        final List<Subobject> subobjs = new ArrayList<>(ipPrefixes.size());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder();
        for (final String ipPrefix : ipPrefixes) {
            subobjBuilder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                    new IpPrefix(new Ipv4Prefix(ipPrefix))).build()).build());
            subobjs.add(subobjBuilder.build());
        }
        return new EroBuilder().setSubobject(subobjs).build();
    }

    String getLastEroIpPrefix(final Ero ero) {
        return ((IpPrefixCase) ero.getSubobject().get(ero.getSubobject().size() - 1).getSubobjectType()).getIpPrefix()
                .getIpPrefix().getIpv4Prefix().getValue();
    }

    protected Open getLocalPref() {
        return this.localPrefs;
    }

    protected Open getRemotePref() {
        return this.remotePrefs;
    }

    protected PCEPSessionListener getSessionListener() {
        return this.manager.getSessionListener();
    }

    protected final PCEPSessionImpl getPCEPSession(final Open localOpen, final Open remoteOpen) {
        return this.neg.createSession(this.clientListener, localOpen, remoteOpen);
    }
}
