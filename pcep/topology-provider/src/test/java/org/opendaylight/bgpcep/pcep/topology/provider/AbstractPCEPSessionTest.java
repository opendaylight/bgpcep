/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class AbstractPCEPSessionTest<T extends TopologySessionListenerFactory> extends AbstractDataBrokerTest {

    protected static final String TEST_TOPOLOGY_NAME = "testtopo";
    protected static final InstanceIdentifier<Topology> TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class).child(
            Topology.class, new TopologyKey(new TopologyId(TEST_TOPOLOGY_NAME))).build();
    protected static final String TEST_ADDRESS = "127.0.0.1";
    protected static final NodeId NODE_ID = new NodeId("pcc://" + TEST_ADDRESS);
    protected static final String TEST_LSP_NAME = "tunnel0";
    protected static final String IPV4_MASK = "/32";
    protected static final String ERO_IP_PREFIX = TEST_ADDRESS + IPV4_MASK;
    protected static final String NEW_DESTINATION_ADDRESS = "127.0.1.0";
    protected static final String DST_IP_PREFIX = NEW_DESTINATION_ADDRESS + IPV4_MASK;
    protected static final short DEAD_TIMER = 30;
    protected static final short KEEP_ALIVE = 10;

    protected List<Notification> receivedMsgs;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private Channel clientListener;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private ChannelFuture channelFuture;

    private T listenerFactory;

    private final Open localPrefs = new OpenBuilder().setDeadTimer((short) 30).setKeepalive((short) 10).setSessionId((short) 0).build();

    private final Open remotePrefs = localPrefs;

    protected ServerSessionManager manager;

    protected NetworkTopologyPcepService topologyRpcs;

    private DefaultPCEPSessionNegotiator neg;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.receivedMsgs = new ArrayList<>();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                AbstractPCEPSessionTest.this.receivedMsgs.add((Notification) args[0]);
                return channelFuture;
            }
        }).when(this.clientListener).writeAndFlush(any(Notification.class));
        doReturn(null).when(this.channelFuture).addListener(Mockito.<GenericFutureListener<? extends Future<? super Void>>>any());
        doReturn("TestingChannel").when(this.clientListener).toString();
        doReturn(this.pipeline).when(this.clientListener).pipeline();
        doReturn(this.pipeline).when(this.pipeline).replace(any(ChannelHandler.class), any(String.class), any(ChannelHandler.class));
        doReturn(this.eventLoop).when(this.clientListener).eventLoop();
        doReturn(null).when(this.eventLoop).schedule(any(Runnable.class), any(long.class), any(TimeUnit.class));
        doReturn(true).when(this.clientListener).isActive();
        final SocketAddress ra = new InetSocketAddress(TEST_ADDRESS, 4189);
        doReturn(ra).when(this.clientListener).remoteAddress();
        final SocketAddress la = new InetSocketAddress(TEST_ADDRESS, 30000);
        doReturn(la).when(this.clientListener).localAddress();

        doReturn(mock(ChannelFuture.class)).when(this.clientListener).close();

        this.listenerFactory = (T) ((Class)((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).newInstance();
        this.manager = new ServerSessionManager(getDataBroker(), TOPO_IID, this.listenerFactory);

        this.neg = new DefaultPCEPSessionNegotiator(mock(Promise.class), this.clientListener, this.manager.getSessionListener(), (short) 1, 5, this.localPrefs);
        this.topologyRpcs = new TopologyRPCs(this.manager);
    }

    @After
    public void tearDown() throws TransactionCommitFailedException {
        this.manager.close();
    }

    protected Optional<Topology> getTopology() throws InterruptedException, ExecutionException {
        try (ReadOnlyTransaction t = getDataBroker().newReadOnlyTransaction()) {
            return t.read(LogicalDatastoreType.OPERATIONAL, TOPO_IID).get();
        }
    }

    protected Ero createEroWithIpPrefixes(final List<String> ipPrefixes) {
        final List<Subobject> subobjs = new ArrayList<Subobject>(ipPrefixes.size());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder();
        for (final String ipPrefix : ipPrefixes) {
            subobjBuilder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(new Ipv4Prefix(ipPrefix))).build()).build());
            subobjs.add(subobjBuilder.build());
        }
        return new EroBuilder().setSubobject(subobjs).build();
    }

    protected String getLastEroIpPrefix(final Ero ero) {
        return ((IpPrefixCase)ero.getSubobject().get(ero.getSubobject().size() - 1).getSubobjectType()).getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
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

    protected final PCEPSession getPCEPSession(final Open localOpen, final Open remoteOpen) {
        return neg.createSession(this.clientListener, localOpen, remoteOpen);
    }
}
