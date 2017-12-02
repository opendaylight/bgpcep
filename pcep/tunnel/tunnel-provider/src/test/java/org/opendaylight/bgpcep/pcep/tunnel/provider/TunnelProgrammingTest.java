/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.tunnel.provider;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcepCreateP2pTunnelInput1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcepCreateP2pTunnelInput1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcepUpdateTunnelInput1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcepUpdateTunnelInput1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.add.lsp.args.Arguments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.p2p.rev130819.tunnel.p2p.path.cfg.attributes.ExplicitHops;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.p2p.rev130819.tunnel.p2p.path.cfg.attributes.ExplicitHopsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.ExplicitHops1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.ExplicitHops1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.tunnel.pcep.supporting.node.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.create.p2p.tunnel.input.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.create.p2p.tunnel.input.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.IgpTerminationPointAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.IpBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TunnelProgrammingTest extends AbstractConcurrentDataBrokerTest {

    private static final TopologyId TOPOLOGY_ID = new TopologyId("tunnel-topo");
    private static final InstanceIdentifier<Topology> TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(TOPOLOGY_ID)).build();

    private static final String NODE1_IPV4 = "127.0.0.1";
    private static final NodeId NODE1_ID = new NodeId("pcc://" + NODE1_IPV4);
    private static final TpId TP1_ID = new TpId(NODE1_IPV4);

    private static final String NODE2_IPV4 = "127.0.0.10";
    private static final NodeId NODE2_ID = new NodeId("pcc://" + NODE2_IPV4);
    private static final TpId TP2_ID = new TpId(NODE2_IPV4);

    private static final LinkId LINK1_ID = new LinkId("link1");

    private static final String IPV4_PREFIX1 = "195.20.160.40/32";
    private static final String IPV4_PREFIX2 = "201.20.160.43/32";

    @Mock
    private NetworkTopologyPcepService topologyService;
    @Mock
    private InstructionScheduler scheduler;
    @Mock
    private ListenableFuture<Instruction> instructionFuture;
    @Mock
    private Instruction instruction;

    private TunnelProgramming tunnelProgramming;

    private AddLspInput addLspInput;
    private UpdateLspInput updateLspInput;
    private RemoveLspInput removeLspInput;

    @Mock
    private ListenableFuture<RpcResult<AddLspOutput>> futureAddLspOutput;
    @Mock
    private ListenableFuture<RpcResult<UpdateLspOutput>> futureUpdateLspOutput;
    @Mock
    private ListenableFuture<RpcResult<RemoveLspOutput>> futureRemoveLspOutput;

    private static Node createNode(final NodeId nodeId, final TpId tpId, final String ipv4Address) {
        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setTpId(tpId);
        tpBuilder.setKey(new TerminationPointKey(tpId));
        tpBuilder.addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                .setIgpTerminationPointAttributes(new IgpTerminationPointAttributesBuilder()
                        .setTerminationPointType(new IpBuilder()
                                .setIpAddress(Collections.singletonList(new IpAddress(new Ipv4Address(ipv4Address))))
                                .build()).build()).build());
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.setKey(new NodeKey(nodeId));
        nodeBuilder.setTerminationPoint(Lists.newArrayList(tpBuilder.build()));
        final SupportingNode supportingNode = new SupportingNodeBuilder()
                .setKey(new SupportingNodeKey(nodeId, new TopologyId("dummy")))
                .addAugmentation(SupportingNode1.class, new SupportingNode1Builder()
                        .setPathComputationClient(new PathComputationClientBuilder()
                                .setControlling(true).build()).build()).build();
        nodeBuilder.setSupportingNode(Lists.newArrayList(supportingNode));
        return nodeBuilder.build();
    }

    private static ExplicitHops createExplicitHop(final String ipv4Prefix) {
        final ExplicitHopsBuilder explcitHopsBuilder = new ExplicitHopsBuilder();
        explcitHopsBuilder.addAugmentation(ExplicitHops1.class, new ExplicitHops1Builder()
                .setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder()
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix(ipv4Prefix))).build()).build()).build());
        return explcitHopsBuilder.build();
    }

    @Before
    public void setUp() throws SchedulerException, InterruptedException, ExecutionException,
            TransactionCommitFailedException {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(true).when(this.instruction).checkedExecutionStart();
        Mockito.doNothing().when(this.instruction).executionCompleted(InstructionStatus.Failed, null);
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.instructionFuture).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureAddLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureUpdateLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureRemoveLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            TunnelProgrammingTest.this.addLspInput = (AddLspInput) invocation.getArguments()[0];
            return TunnelProgrammingTest.this.futureAddLspOutput;
        }).when(this.topologyService).addLsp(Mockito.any(AddLspInput.class));
        Mockito.doAnswer(invocation -> {
            TunnelProgrammingTest.this.updateLspInput = (UpdateLspInput) invocation.getArguments()[0];
            return TunnelProgrammingTest.this.futureUpdateLspOutput;
        }).when(this.topologyService).updateLsp(Mockito.any(UpdateLspInput.class));
        Mockito.doAnswer(invocation -> {
            TunnelProgrammingTest.this.removeLspInput = (RemoveLspInput) invocation.getArguments()[0];
            return TunnelProgrammingTest.this.futureRemoveLspOutput;
        }).when(this.topologyService).removeLsp(Mockito.any(RemoveLspInput.class));
        Mockito.doReturn(this.instruction).when(this.instructionFuture).get();
        Mockito.doReturn(true).when(this.instructionFuture).isDone();
        Mockito.doReturn(this.instructionFuture).when(this.scheduler)
                .scheduleInstruction(Mockito.any(SubmitInstructionInput.class));

        createInitialTopology();
        this.tunnelProgramming = new TunnelProgramming(this.scheduler, getDataBroker(), this.topologyService);
    }

    @Test
    public void testTunnelProgramming() throws TransactionCommitFailedException {
        final Bandwidth bwd = new Bandwidth(new byte[]{0x00, 0x00, 0x00, (byte) 0xff});
        final ClassType classType = new ClassType((short) 1);
        final String tunnelName = "create-tunnel";
        final NetworkTopologyRef topologyRef = new NetworkTopologyRef(TOPO_IID);
        // create tunnel
        final PcepCreateP2pTunnelInputBuilder createInputBuilder = new PcepCreateP2pTunnelInputBuilder();
        createInputBuilder.setDestination(new DestinationBuilder().setNode(NODE2_ID).setTp(TP2_ID).build());
        createInputBuilder.setSource(new SourceBuilder().setNode(NODE1_ID).setTp(TP1_ID).build());
        createInputBuilder.setNetworkTopologyRef(topologyRef);
        createInputBuilder.setBandwidth(bwd);
        createInputBuilder.setClassType(classType);
        createInputBuilder.setSymbolicPathName(tunnelName);
        createInputBuilder.setExplicitHops(Lists.newArrayList());
        createInputBuilder.addAugmentation(PcepCreateP2pTunnelInput1.class, new PcepCreateP2pTunnelInput1Builder()
                .setAdministrativeStatus(AdministrativeStatus.Active).build());
        this.tunnelProgramming.pcepCreateP2pTunnel(createInputBuilder.build());
        //check add-lsp input
        Assert.assertNotNull(this.addLspInput);
        Assert.assertEquals(tunnelName, this.addLspInput.getName());
        final Arguments agrs = this.addLspInput.getArguments();
        Assert.assertNotNull(agrs);
        Assert.assertEquals(bwd, agrs.getBandwidth().getBandwidth());
        Assert.assertEquals(classType, agrs.getClassType().getClassType());
        final Ipv4 ipv4Endpoints = ((Ipv4Case) agrs.getEndpointsObj().getAddressFamily()).getIpv4();
        Assert.assertEquals(NODE1_IPV4, ipv4Endpoints.getSourceIpv4Address().getValue());
        Assert.assertEquals(NODE2_IPV4, ipv4Endpoints.getDestinationIpv4Address().getValue());
        Assert.assertEquals(NODE1_ID.getValue(), this.addLspInput.getNode().getValue());
        createLink();

        // update tunnel
        final PcepUpdateTunnelInputBuilder updateInputBuilder = new PcepUpdateTunnelInputBuilder();
        updateInputBuilder.setNetworkTopologyRef(topologyRef);
        updateInputBuilder.setBandwidth(bwd);
        updateInputBuilder.setClassType(classType);
        updateInputBuilder.setExplicitHops(Lists.newArrayList(createExplicitHop(IPV4_PREFIX1),
                createExplicitHop(IPV4_PREFIX2)));
        updateInputBuilder.setLinkId(LINK1_ID);
        updateInputBuilder.addAugmentation(PcepUpdateTunnelInput1.class, new PcepUpdateTunnelInput1Builder()
                .setAdministrativeStatus(AdministrativeStatus.Active).build());
        this.tunnelProgramming.pcepUpdateTunnel(updateInputBuilder.build());
        //check update-lsp input
        Assert.assertNotNull(this.updateLspInput);
        Assert.assertEquals(LINK1_ID.getValue(), this.updateLspInput.getName());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp
                .args.Arguments updArgs = this.updateLspInput.getArguments();
        Assert.assertEquals(2, updArgs.getEro().getSubobject().size());
        final List<Subobject> subObjects = updArgs.getEro().getSubobject();
        final IpPrefixCase prefix1 = (IpPrefixCase) subObjects.get(0).getSubobjectType();
        final IpPrefixCase prefix2 = (IpPrefixCase) subObjects.get(1).getSubobjectType();
        Assert.assertEquals(IPV4_PREFIX1, prefix1.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue());
        Assert.assertEquals(IPV4_PREFIX2, prefix2.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue());

        // delete tunnel
        final PcepDestroyTunnelInputBuilder destroyInputBuilder = new PcepDestroyTunnelInputBuilder();
        destroyInputBuilder.setLinkId(LINK1_ID);
        destroyInputBuilder.setNetworkTopologyRef(topologyRef);
        this.tunnelProgramming.pcepDestroyTunnel(destroyInputBuilder.build());
        Assert.assertNotNull(this.removeLspInput);
        Assert.assertEquals(LINK1_ID.getValue(), this.removeLspInput.getName());
        Assert.assertEquals(NODE1_ID.getValue(), this.removeLspInput.getNode().getValue());
    }

    private void createInitialTopology() throws TransactionCommitFailedException {
        final TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setKey(new TopologyKey(TOPOLOGY_ID));
        topologyBuilder.setServerProvided(true);
        topologyBuilder.setTopologyId(TOPOLOGY_ID);
        topologyBuilder.setNode(Lists.newArrayList(createNode(NODE1_ID, TP1_ID, NODE1_IPV4),
                createNode(NODE2_ID, TP2_ID, NODE2_IPV4)));
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, TOPO_IID, topologyBuilder.build(), true);
        wTx.submit().checkedGet();
    }

    private void createLink() throws TransactionCommitFailedException {
        final LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setSource(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                .rev131021.link.attributes.SourceBuilder().setSourceNode(NODE1_ID).setSourceTp(TP1_ID).build());
        linkBuilder.setDestination(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology
                .rev131021.link.attributes.DestinationBuilder().setDestNode(NODE2_ID).setDestTp(TP2_ID).build());
        linkBuilder.setLinkId(LINK1_ID);
        linkBuilder.setKey(new LinkKey(LINK1_ID));
        linkBuilder.addAugmentation(Link1.class, new Link1Builder().setSymbolicPathName(LINK1_ID.getValue()).build());
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, TOPO_IID.builder().child(Link.class, new LinkKey(LINK1_ID)).build(),
                linkBuilder.build(), true);
        wTx.submit().checkedGet();
    }

}
