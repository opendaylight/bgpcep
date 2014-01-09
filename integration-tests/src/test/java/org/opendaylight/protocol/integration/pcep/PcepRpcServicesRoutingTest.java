/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.pcep;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(PaxExam.class)
@ExamReactorStrategy(org.ops4j.pax.exam.spi.reactors.PerClass.class)
public class PcepRpcServicesRoutingTest extends AbstractPcepOsgiTest {

    @Test
    public void testRoutedRpcNetworkTopologyPcepService() throws Exception {
        final NetworkTopologyPcepService pcepService1 = mock(NetworkTopologyPcepService.class, "First pcep Service");
        final NetworkTopologyPcepService pcepService2 = mock(NetworkTopologyPcepService.class, "Second pcep Service");

        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology =
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId("Topo1"))).toInstance();

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> firstReg = session.addRoutedRpcImplementation(NetworkTopologyPcepService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(TopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 =
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId("Topo2"))).toInstance();

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> secondReg = session.addRoutedRpcImplementation(NetworkTopologyPcepService.class, pcepService2);
                secondReg.registerPath(TopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(BindingAwareBroker.ConsumerContext session) {
                NetworkTopologyPcepService consumerPcepService = session.getRpcService(NetworkTopologyPcepService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                AddLspInput addLspInput = getAddLspInput(topology);
                consumerPcepService.addLsp(addLspInput);

                verify(pcepService1).addLsp(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getAddLspInput(topology2);

                consumerPcepService.addLsp(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).addLsp(addLspInput);
            }

            private AddLspInput getAddLspInput(InstanceIdentifier<Topology> topology) {
                AddLspInputBuilder builder = new AddLspInputBuilder();
                builder.setTopologyIdentifier(new TopologyRef(topology));
                builder.setArguments(new ArgumentsBuilder().build());
                return builder.build();
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

    @Test
    public void testRoutedRpcNetworkTopologyPcepProgrammingService() throws Exception {
        final NetworkTopologyPcepProgrammingService pcepService1 = mock(NetworkTopologyPcepProgrammingService.class, "First pcep program Service");
        final NetworkTopologyPcepProgrammingService pcepService2 = mock(NetworkTopologyPcepProgrammingService.class, "Second pcep program Service");

        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology =
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId("Topo1"))).toInstance();

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> firstReg = session.addRoutedRpcImplementation(NetworkTopologyPcepProgrammingService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(TopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 =
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId("Topo2"))).toInstance();

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> secondReg = session.addRoutedRpcImplementation(NetworkTopologyPcepProgrammingService.class, pcepService2);
                secondReg.registerPath(TopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(BindingAwareBroker.ConsumerContext session) {
                NetworkTopologyPcepProgrammingService consumerPcepService = session.getRpcService(NetworkTopologyPcepProgrammingService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                SubmitAddLspInput addLspInput = getSubmitAddLspInput(topology);
                consumerPcepService.submitAddLsp(addLspInput);

                verify(pcepService1).submitAddLsp(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getSubmitAddLspInput(topology2);

                consumerPcepService.submitAddLsp(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitAddLsp(addLspInput);
            }

            private SubmitAddLspInput getSubmitAddLspInput(InstanceIdentifier<Topology> topology) {
                SubmitAddLspInputBuilder builder = new SubmitAddLspInputBuilder();
                builder.setTopologyIdentifier(new TopologyRef(topology));
                builder.setArguments(new ArgumentsBuilder().build());
                return builder.build();
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

    @Test
    public void testRoutedRpcTopologyTunnelPcepProgrammingService() throws Exception {
        final TopologyTunnelPcepProgrammingService pcepService1 = mock(TopologyTunnelPcepProgrammingService.class, "First pcep program Service");
        final TopologyTunnelPcepProgrammingService pcepService2 = mock(TopologyTunnelPcepProgrammingService.class, "Second pcep program Service");

        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology =
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId("Topo1"))).toInstance();

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> firstReg = session.addRoutedRpcImplementation(TopologyTunnelPcepProgrammingService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.programming.rev131102.TopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 =
                InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId("Topo2"))).toInstance();

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> secondReg = session.addRoutedRpcImplementation(TopologyTunnelPcepProgrammingService.class, pcepService2);
                secondReg.registerPath(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.programming.rev131102.TopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(BindingAwareBroker.ConsumerContext session) {
                TopologyTunnelPcepProgrammingService consumerPcepService = session.getRpcService(TopologyTunnelPcepProgrammingService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                PcepCreateP2pTunnelInput addLspInput = getPcepCreateP2pTunnelInput(topology);
                consumerPcepService.pcepCreateP2pTunnel(addLspInput);

                verify(pcepService1).pcepCreateP2pTunnel(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getPcepCreateP2pTunnelInput(topology2);

                consumerPcepService.pcepCreateP2pTunnel(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).pcepCreateP2pTunnel(addLspInput);
            }

            public PcepCreateP2pTunnelInput getPcepCreateP2pTunnelInput(InstanceIdentifier<Topology> topology) {
                PcepCreateP2pTunnelInputBuilder builder = new PcepCreateP2pTunnelInputBuilder();
                builder.setTopologyIdentifier(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.programming.rev131102.TopologyRef(topology));
                return builder.build();
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

}
