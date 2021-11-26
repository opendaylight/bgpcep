/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcepCreateP2pTunnelInput1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcepUpdateTunnelInput1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.add.lsp.args.Arguments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.p2p.rev130819.tunnel.p2p.path.cfg.attributes.ExplicitHops;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.p2p.rev130819.tunnel.p2p.path.cfg.attributes.ExplicitHopsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.ExplicitHops1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.SupportingNode1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.tunnel.pcep.supporting.node.attributes.PathComputationClientBuilder;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.IgpTerminationPointAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.IpBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
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
    @Mock
    private ClusterSingletonServiceProvider cssp;
    @Mock
    private RpcProviderService rpr;
    @Mock
    private RpcConsumerRegistry rpcs;
    @Mock
    private BundleContext bundleContext;

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
        return new NodeBuilder()
            .setNodeId(nodeId)
            .setTerminationPoint(BindingMap.of(new TerminationPointBuilder()
                .setTpId(tpId)
                .addAugmentation(new TerminationPoint1Builder()
                    .setIgpTerminationPointAttributes(new IgpTerminationPointAttributesBuilder()
                        .setTerminationPointType(new IpBuilder()
                            .setIpAddress(Set.of(new IpAddress(new Ipv4Address(ipv4Address))))
                            .build())
                        .build())
                    .build())
                .build()))
            .setSupportingNode(BindingMap.of(new SupportingNodeBuilder()
                .setTopologyRef(new TopologyId("dummy"))
                .setNodeRef(nodeId)
                .addAugmentation(new SupportingNode1Builder()
                    .setPathComputationClient(new PathComputationClientBuilder().setControlling(true).build())
                    .build())
                .build()))
            .build();
    }

    private static ExplicitHops createExplicitHop(final String ipv4Prefix, final Uint32 order) {
        return new ExplicitHopsBuilder()
                .setOrder(order)
                .addAugmentation(new ExplicitHops1Builder()
                    .setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder()
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix(ipv4Prefix))).build()).build())
                    .build())
                .build();
    }

    @Before
    public void setUp() throws SchedulerException, InterruptedException, ExecutionException {
        doReturn(true).when(instruction).checkedExecutionStart();
        doNothing().when(instruction).executionCompleted(InstructionStatus.Failed, null);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(instructionFuture).addListener(ArgumentMatchers.any(Runnable.class), Mockito.any(Executor.class));
        doReturn(false).when(futureAddLspOutput).isCancelled();
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(futureAddLspOutput).addListener(any(Runnable.class), any(Executor.class));
        doReturn(false).when(futureUpdateLspOutput).isCancelled();
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(futureUpdateLspOutput).addListener(any(Runnable.class), any(Executor.class));
        doReturn(false).when(futureRemoveLspOutput).isCancelled();
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(futureRemoveLspOutput).addListener(any(Runnable.class), any(Executor.class));
        doAnswer(invocation -> {
            addLspInput = invocation.getArgument(0);
            return futureAddLspOutput;
        }).when(topologyService).addLsp(any(AddLspInput.class));
        doAnswer(invocation -> {
            updateLspInput = invocation.getArgument(0);
            return futureUpdateLspOutput;
        }).when(topologyService).updateLsp(any(UpdateLspInput.class));
        doAnswer(invocation -> {
            removeLspInput = invocation.getArgument(0);
            return futureRemoveLspOutput;
        }).when(topologyService).removeLsp(any(RemoveLspInput.class));
        doReturn(instruction).when(instructionFuture).get();
        doReturn(true).when(instructionFuture).isDone();
        doReturn(instructionFuture).when(scheduler)
                .scheduleInstruction(any(SubmitInstructionInput.class));

        doReturn(topologyService).when(rpcs)
                .getRpcService(NetworkTopologyPcepService.class);

        createInitialTopology();
        tunnelProgramming = new TunnelProgramming(scheduler,
            new TunnelProviderDependencies(getDataBroker(), cssp, rpr, rpcs, bundleContext));
    }

    @Test
    public void testTunnelProgramming() throws InterruptedException, ExecutionException {
        final Bandwidth bwd = new Bandwidth(new byte[] { 0x00, 0x00, 0x00, (byte) 0xff });
        final ClassType classType = new ClassType(Uint8.ONE);
        final String tunnelName = "create-tunnel";
        final NetworkTopologyRef topologyRef = new NetworkTopologyRef(TOPO_IID);
        // create tunnel
        tunnelProgramming.pcepCreateP2pTunnel(new PcepCreateP2pTunnelInputBuilder()
            .setDestination(new DestinationBuilder().setNode(NODE2_ID).setTp(TP2_ID).build())
            .setSource(new SourceBuilder().setNode(NODE1_ID).setTp(TP1_ID).build())
            .setNetworkTopologyRef(topologyRef)
            .setBandwidth(bwd)
            .setClassType(classType)
            .setSymbolicPathName(tunnelName)
            .addAugmentation(new PcepCreateP2pTunnelInput1Builder()
                .setAdministrativeStatus(AdministrativeStatus.Active)
                .build())
            .build());
        //check add-lsp input
        assertNotNull(addLspInput);
        assertEquals(tunnelName, addLspInput.getName());
        final Arguments agrs = addLspInput.getArguments();
        assertNotNull(agrs);
        assertEquals(bwd, agrs.getBandwidth().getBandwidth());
        assertEquals(classType, agrs.getClassType().getClassType());
        final Ipv4 ipv4Endpoints = ((Ipv4Case) agrs.getEndpointsObj().getAddressFamily()).getIpv4();
        assertEquals(NODE1_IPV4, ipv4Endpoints.getSourceIpv4Address().getValue());
        assertEquals(NODE2_IPV4, ipv4Endpoints.getDestinationIpv4Address().getValue());
        assertEquals(NODE1_ID.getValue(), addLspInput.getNode().getValue());
        createLink();

        // update tunnel
        tunnelProgramming.pcepUpdateTunnel(new PcepUpdateTunnelInputBuilder()
            .setNetworkTopologyRef(topologyRef)
            .setBandwidth(bwd)
            .setClassType(classType)
            // We assert on explicit order
            .setExplicitHops(BindingMap.ordered(
                createExplicitHop(IPV4_PREFIX1, Uint32.ONE),
                createExplicitHop(IPV4_PREFIX2, Uint32.TWO)))
            .setLinkId(LINK1_ID)
            .addAugmentation(new PcepUpdateTunnelInput1Builder()
                .setAdministrativeStatus(AdministrativeStatus.Active)
                .build())
            .build());
        //check update-lsp input
        assertNotNull(updateLspInput);
        assertEquals(LINK1_ID.getValue(), updateLspInput.getName());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.update.lsp
                .args.Arguments updArgs = updateLspInput.getArguments();
        assertEquals(2, updArgs.getEro().nonnullSubobject().size());
        final List<Subobject> subObjects = updArgs.getEro().nonnullSubobject();
        final IpPrefixCase prefix1 = (IpPrefixCase) subObjects.get(0).getSubobjectType();
        final IpPrefixCase prefix2 = (IpPrefixCase) subObjects.get(1).getSubobjectType();
        assertEquals(IPV4_PREFIX1, prefix1.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue());
        assertEquals(IPV4_PREFIX2, prefix2.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue());

        // delete tunnel
        final PcepDestroyTunnelInputBuilder destroyInputBuilder = new PcepDestroyTunnelInputBuilder();
        destroyInputBuilder.setLinkId(LINK1_ID);
        destroyInputBuilder.setNetworkTopologyRef(topologyRef);
        tunnelProgramming.pcepDestroyTunnel(destroyInputBuilder.build());
        assertNotNull(removeLspInput);
        assertEquals(LINK1_ID.getValue(), removeLspInput.getName());
        assertEquals(NODE1_ID, removeLspInput.getNode());
    }

    private void createInitialTopology() throws InterruptedException, ExecutionException {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, TOPO_IID, new TopologyBuilder()
            .setTopologyId(TOPOLOGY_ID)
            .setServerProvided(true)
            .setTopologyId(TOPOLOGY_ID)
            .setNode(BindingMap.of(createNode(NODE1_ID, TP1_ID, NODE1_IPV4), createNode(NODE2_ID, TP2_ID, NODE2_IPV4)))
            .build());
        wTx.commit().get();
    }

    private void createLink() throws InterruptedException, ExecutionException {
        final WriteTransaction wTx = getDataBroker().newWriteOnlyTransaction();
        wTx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL,
            TOPO_IID.child(Link.class, new LinkKey(LINK1_ID)),
            new LinkBuilder()
                .setSource(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                    .link.attributes.SourceBuilder().setSourceNode(NODE1_ID).setSourceTp(TP1_ID).build())
                .setDestination(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021
                    .link.attributes.DestinationBuilder().setDestNode(NODE2_ID).setDestTp(TP2_ID).build())
                .setLinkId(LINK1_ID)
                .addAugmentation(new Link1Builder().setSymbolicPathName(LINK1_ID.getValue()).build())
                .build());
        wTx.commit().get();
    }
}
