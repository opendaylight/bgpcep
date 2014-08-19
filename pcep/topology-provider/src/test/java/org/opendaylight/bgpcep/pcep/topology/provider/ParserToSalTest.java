/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLspTlvs;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiator;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public class ParserToSalTest extends AbstractDataBrokerTest {

    private static final String TEST_TOPOLOGY_NAME = "testtopo";
    private static final InstanceIdentifier<Topology> TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class).child(
            Topology.class, new TopologyKey(new TopologyId(TEST_TOPOLOGY_NAME))).toInstance();
    private static final String TEST_ADDRESS = "127.0.0.1";
    private static final NodeId NODE_ID = new NodeId("pcc://" + TEST_ADDRESS);
    private static final String TEST_LSP_NAME = "tunnel0";
    private static final String IPV4_MASK = "/32";
    private static final String ERO_IP_PREFIX = TEST_ADDRESS + IPV4_MASK;
    private static final String NEW_DESTINATION_ADDRESS = "127.0.1.0";
    private static final String DST_IP_PREFIX = NEW_DESTINATION_ADDRESS + IPV4_MASK;

    private List<Notification> receivedMsgs;

    private PCEPSessionImpl session;

    @Mock
    private EventLoop eventLoop;

    @Mock
    private Channel clientListener;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private ChannelFuture channelFuture;

    private final Open localPrefs = new OpenBuilder().setDeadTimer((short) 30).setKeepalive((short) 10).setTlvs(
            new TlvsBuilder().addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder().build()).build()).build()).build();

    private Pcrpt rptmsg;

    private ServerSessionManager manager;

    private Stateful07TopologySessionListener listener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.receivedMsgs = new ArrayList<>();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                ParserToSalTest.this.receivedMsgs.add((Notification) args[0]);
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

        this.manager = new ServerSessionManager(getDataBroker(), TOPO_IID, new Stateful07TopologySessionListenerFactory());
        this.listener = (Stateful07TopologySessionListener) manager.getSessionListener();

        final DefaultPCEPSessionNegotiator neg = new DefaultPCEPSessionNegotiator(mock(Promise.class), this.clientListener, this.manager.getSessionListener(), (short) 1, 5, this.localPrefs);
        this.session = neg.createSession(this.clientListener, this.localPrefs, this.localPrefs);

        final List<Reports> reports = Lists.newArrayList(new ReportsBuilder().setPath(new PathBuilder().setEro(new EroBuilder().build()).build()).setLsp(
                new LspBuilder().setPlspId(new PlspId(5L)).setSync(false).setRemove(false).setTlvs(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1L)).build()).setSymbolicPathName(
                                new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(new byte[] { 22, 34 })).build()).build()).build()).build());
        this.rptmsg = new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();

        assertTrue(getTopology().isPresent());
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        this.manager.close();
    }

    @Test
    public void testUnknownLsp() throws Exception {
        this.session.sessionUp();
        this.session.handleMessage(this.rptmsg);
        final Topology topology = getTopology().get();
        assertFalse(topology.getNode().isEmpty());
    }

    @Test
    public void testStateful07TopologySessionListener() throws Exception {
        final NetworkTopologyPcepService rpcs = new TopologyRPCs(this.manager);
        this.listener.onSessionUp(this.session);

        // add-lsp
        final ArgumentsBuilder argsBuilder = new ArgumentsBuilder();
        final Ipv4CaseBuilder ipv4Builder = new Ipv4CaseBuilder();
        ipv4Builder.setIpv4(new Ipv4Builder().setSourceIpv4Address(new Ipv4Address(TEST_ADDRESS)).setDestinationIpv4Address(new Ipv4Address(TEST_ADDRESS)).build());
        argsBuilder.setEndpointsObj(new EndpointsObjBuilder().setAddressFamily(ipv4Builder.build()).build());
        argsBuilder.setEro(createEroWithIpPrefixes(Lists.newArrayList(ERO_IP_PREFIX)));
        argsBuilder.addAugmentation(Arguments2.class, new Arguments2Builder().setLsp(new LspBuilder().setDelegate(true).setAdministrative(true).build()).build());
        final AddLspInput input = new AddLspInputBuilder().setName(TEST_LSP_NAME).setArguments(argsBuilder.build()).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        rpcs.addLsp(input);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Pcinitiate);
        final Pcinitiate pcinitiate = (Pcinitiate) this.receivedMsgs.get(0);
        final Requests req = pcinitiate.getPcinitiateMessage().getRequests().get(0);
        final long srpId = req.getSrp().getOperationId().getValue();
        final InetAddress inetAddress = InetAddress.getByName(TEST_ADDRESS);
        final Tlvs tlvs = createLspTlvs(req.getLsp().getPlspId().getValue(), true,
                inetAddress, inetAddress, inetAddress);
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(req.getLsp()).setTlvs(tlvs).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.of(MsgBuilderUtil.createSrp(srpId)), MsgBuilderUtil.createPath(req.getEro().getSubobject()));

        final Optional<Topology> topoOptional = getTopology();
        assertTrue(topoOptional.isPresent());
        Topology topology = topoOptional.get();
        assertEquals(1, topology.getNode().size());
        final Node1 node = topology.getNode().get(0).getAugmentation(Node1.class);
        assertNotNull(node);
        PathComputationClient pcc = node.getPathComputationClient();
        assertEquals(TEST_ADDRESS, pcc.getIpAddress().getIpv4Address().getValue());
        // reported lsp so far empty, has not received response (PcRpt) yet
        assertEquals(0, pcc.getReportedLsp().size());
        this.listener.onMessage(this.session, pcRpt);
        // check created lsp
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(1, pcc.getReportedLsp().size());
        ReportedLsp reportedLsp = pcc.getReportedLsp().get(0);
        assertEquals(TEST_LSP_NAME, reportedLsp.getName());
        assertEquals(1, reportedLsp.getPath().size());
        Path path = reportedLsp.getPath().get(0);
        assertEquals(1, path.getEro().getSubobject().size());
        assertEquals(ERO_IP_PREFIX, getLastEroIpPrefix(path.getEro()));

        // update-lsp
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder updArgsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder();
        updArgsBuilder.setEro(createEroWithIpPrefixes(Lists.newArrayList(ERO_IP_PREFIX, DST_IP_PREFIX)));
        updArgsBuilder.addAugmentation(Arguments3.class, new Arguments3Builder().setLsp(new LspBuilder().setDelegate(true).setAdministrative(true).build()).build());
        final UpdateLspInput update = new UpdateLspInputBuilder().setArguments(updArgsBuilder.build()).setName(TEST_LSP_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        rpcs.updateLsp(update);
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Pcupd);
        final Pcupd updateMsg = (Pcupd) this.receivedMsgs.get(1);
        final Updates upd = updateMsg.getPcupdMessage().getUpdates().get(0);
        final long srpId2 = upd.getSrp().getOperationId().getValue();
        final Tlvs tlvs2 = createLspTlvs(upd.getLsp().getPlspId().getValue(), false,
                InetAddress.getByName(NEW_DESTINATION_ADDRESS), inetAddress, inetAddress);
        final Pcrpt pcRpt2 = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(upd.getLsp()).setTlvs(tlvs2).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.of(MsgBuilderUtil.createSrp(srpId2)), MsgBuilderUtil.createPath(upd.getPath().getEro().getSubobject()));
        this.listener.onMessage(this.session, pcRpt2);
        //check updated lsp
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(1, pcc.getReportedLsp().size());
        reportedLsp = pcc.getReportedLsp().get(0);
        assertEquals(TEST_LSP_NAME, reportedLsp.getName());
        assertEquals(1, reportedLsp.getPath().size());
        path = reportedLsp.getPath().get(0);
        assertEquals(2, path.getEro().getSubobject().size());
        assertEquals(DST_IP_PREFIX, getLastEroIpPrefix(path.getEro()));

        // ensure-operational
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.ensure.lsp.operational.args.ArgumentsBuilder ensureArgs = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.ensure.lsp.operational.args.ArgumentsBuilder();
        ensureArgs.addAugmentation(Arguments1.class, new Arguments1Builder().setOperational(OperationalStatus.Active).build());
        final EnsureLspOperationalInput ensure = new EnsureLspOperationalInputBuilder().setArguments(ensureArgs.build()).setName(TEST_LSP_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        final OperationResult result = rpcs.ensureLspOperational(ensure).get().getResult();
        //check result
        assertNull(result.getFailure());

        // remove-lsp
        final RemoveLspInput remove = new RemoveLspInputBuilder().setName(TEST_LSP_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        rpcs.removeLsp(remove);
        assertEquals(3, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(2) instanceof Pcinitiate);
        final Pcinitiate pcinitiate2 = (Pcinitiate) this.receivedMsgs.get(2);
        final Requests req2 = pcinitiate2.getPcinitiateMessage().getRequests().get(0);
        final long srpId3 = req2.getSrp().getOperationId().getValue();
        final Tlvs tlvs3 = createLspTlvs(req2.getLsp().getPlspId().getValue(), false,
                inetAddress, inetAddress, inetAddress);
        final Pcrpt pcRpt3 = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(req2.getLsp()).setTlvs(tlvs3).setRemove(true).setSync(true).setOperational(OperationalStatus.Down).build(), Optional.of(MsgBuilderUtil.createSrp(srpId3)), MsgBuilderUtil.createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(this.session, pcRpt3);
        // check if lsp was removed
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(0, pcc.getReportedLsp().size());
    }

    private Optional<Topology> getTopology() throws InterruptedException, ExecutionException {
        return getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.OPERATIONAL, TOPO_IID).get();
    }

    private Ero createEroWithIpPrefixes(final List<String> ipPrefixes) {
        final List<Subobject> subobjs = new ArrayList<Subobject>(ipPrefixes.size());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder();
        for (final String ipPrefix : ipPrefixes) {
            subobjBuilder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(new Ipv4Prefix(ipPrefix))).build()).build());
            subobjs.add(subobjBuilder.build());
        }
        return new EroBuilder().setSubobject(subobjs).build();
    }

    private String getLastEroIpPrefix(final Ero ero) {
        return ((IpPrefixCase)ero.getSubobject().get(ero.getSubobject().size() - 1).getSubobjectType()).getIpPrefix().getIpPrefix().getIpv4Prefix().getValue();
    }
}
