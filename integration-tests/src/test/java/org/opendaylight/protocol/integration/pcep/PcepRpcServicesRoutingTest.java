/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.integration.pcep;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;

@RunWith(PaxExam.class)
@ExamReactorStrategy(org.ops4j.pax.exam.spi.reactors.PerClass.class)
public class PcepRpcServicesRoutingTest extends AbstractPcepOsgiTest {

    @Test
    public void testRoutedRpcNetworkTopologyPcepService() throws Exception {
        final NetworkTopologyPcepService pcepService1 = mock(NetworkTopologyPcepService.class, "First pcep Service");
        final NetworkTopologyPcepService pcepService2 = mock(NetworkTopologyPcepService.class, "Second pcep Service");

        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo1"))).build();

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> firstReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(NetworkTopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo2"))).build();

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> secondReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepService.class, pcepService2);
                secondReg.registerPath(NetworkTopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
                NetworkTopologyPcepService consumerPcepService = session.getRpcService(NetworkTopologyPcepService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                testAddLspRpce(consumerPcepService);
                testEnsureLspRpce(consumerPcepService);
            }

            private void testAddLspRpce(NetworkTopologyPcepService consumerPcepService) {
                AddLspInput addLspInput = getInputForRpc(topology, AddLspInputBuilder.class, AddLspInput.class);
                consumerPcepService.addLsp(addLspInput);

                verify(pcepService1).addLsp(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getInputForRpc(topology2, AddLspInputBuilder.class, AddLspInput.class);

                consumerPcepService.addLsp(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).addLsp(addLspInput);
            }

            private void testEnsureLspRpce(NetworkTopologyPcepService consumerPcepService) {
                EnsureLspOperationalInput ensureInput = getInputForRpc(topology, EnsureLspOperationalInputBuilder.class,
                        EnsureLspOperationalInput.class);

                consumerPcepService.ensureLspOperational(ensureInput);

                verify(pcepService1).ensureLspOperational(ensureInput);
                verifyZeroInteractions(pcepService2);

                ensureInput = getInputForRpc(topology2, EnsureLspOperationalInputBuilder.class, EnsureLspOperationalInput.class);

                consumerPcepService.ensureLspOperational(ensureInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).ensureLspOperational(ensureInput);
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

    private static <T> T getInputForRpc(final InstanceIdentifier<Topology> topology, Class<?> builderClass, Class<T> builtObjectClass) {
        try {
            Object builderInstance = builderClass.newInstance();
            Method method = builderClass.getMethod("setNetworkTopologyRef", NetworkTopologyRef.class);
            method.invoke(builderInstance, new NetworkTopologyRef(topology));
            return builtObjectClass.cast(builderClass.getMethod("build").invoke(builderInstance));
        } catch (Exception e) {
            throw new RuntimeException("Unable to create instance from " + builderClass, e);
        }
    }

    @Test
    public void testRoutedRpcNetworkTopologyPcepProgrammingService() throws Exception {
        final NetworkTopologyPcepProgrammingService pcepService1 = mock(NetworkTopologyPcepProgrammingService.class,
                "First pcep program Service");
        final NetworkTopologyPcepProgrammingService pcepService2 = mock(NetworkTopologyPcepProgrammingService.class,
                "Second pcep program Service");

        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo1"))).build();

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> firstReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepProgrammingService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(NetworkTopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo2"))).build();

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> secondReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepProgrammingService.class, pcepService2);
                secondReg.registerPath(NetworkTopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
                NetworkTopologyPcepProgrammingService consumerPcepService = session.getRpcService(NetworkTopologyPcepProgrammingService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                testSubmitAddLspRpc(consumerPcepService);
                testSubmitUpdateLspRpc(consumerPcepService);
            }

            private void testSubmitAddLspRpc(NetworkTopologyPcepProgrammingService consumerPcepService) {
                SubmitAddLspInput addLspInput = getInputForRpc(topology, SubmitAddLspInputBuilder.class, SubmitAddLspInput.class);
                consumerPcepService.submitAddLsp(addLspInput);

                verify(pcepService1).submitAddLsp(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getInputForRpc(topology2, SubmitAddLspInputBuilder.class, SubmitAddLspInput.class);

                consumerPcepService.submitAddLsp(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitAddLsp(addLspInput);
            }

            private void testSubmitUpdateLspRpc(NetworkTopologyPcepProgrammingService consumerPcepService) {
                SubmitUpdateLspInput submitLspInput = getInputForRpc(topology, SubmitUpdateLspInputBuilder.class,
                        SubmitUpdateLspInput.class);
                consumerPcepService.submitUpdateLsp(submitLspInput);

                verify(pcepService1).submitUpdateLsp(submitLspInput);
                verifyZeroInteractions(pcepService2);

                submitLspInput = getInputForRpc(topology2, SubmitUpdateLspInputBuilder.class, SubmitUpdateLspInput.class);

                consumerPcepService.submitUpdateLsp(submitLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitUpdateLsp(submitLspInput);
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

    @Test
    public void testRoutedRpcTopologyTunnelPcepProgrammingService() throws Exception {
        final TopologyTunnelPcepProgrammingService pcepService1 = mock(TopologyTunnelPcepProgrammingService.class,
                "First pcep program Service");
        final TopologyTunnelPcepProgrammingService pcepService2 = mock(TopologyTunnelPcepProgrammingService.class,
                "Second pcep program Service");

        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo1"))).build();

        BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> firstReg = session.addRoutedRpcImplementation(
                        TopologyTunnelPcepProgrammingService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(NetworkTopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo2"))).build();

        BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> secondReg = session.addRoutedRpcImplementation(
                        TopologyTunnelPcepProgrammingService.class, pcepService2);
                secondReg.registerPath(NetworkTopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
                TopologyTunnelPcepProgrammingService consumerPcepService = session.getRpcService(TopologyTunnelPcepProgrammingService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                testCreateP2pTunnel(consumerPcepService);
                testDestroyP2pTunnel(consumerPcepService);
            }

            private void testCreateP2pTunnel(TopologyTunnelPcepProgrammingService consumerPcepService) {
                PcepCreateP2pTunnelInput addLspInput = getInputForRpc(topology, PcepCreateP2pTunnelInputBuilder.class,
                        PcepCreateP2pTunnelInput.class);
                consumerPcepService.pcepCreateP2pTunnel(addLspInput);

                verify(pcepService1).pcepCreateP2pTunnel(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getInputForRpc(topology2, PcepCreateP2pTunnelInputBuilder.class, PcepCreateP2pTunnelInput.class);

                consumerPcepService.pcepCreateP2pTunnel(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).pcepCreateP2pTunnel(addLspInput);
            }

            private void testDestroyP2pTunnel(TopologyTunnelPcepProgrammingService consumerPcepService) {
                PcepDestroyTunnelInput addLspInput = getInputForRpc(topology, PcepDestroyTunnelInputBuilder.class,
                        PcepDestroyTunnelInput.class);
                consumerPcepService.pcepDestroyTunnel(addLspInput);

                verify(pcepService1).pcepDestroyTunnel(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getInputForRpc(topology2, PcepDestroyTunnelInputBuilder.class, PcepDestroyTunnelInput.class);

                consumerPcepService.pcepDestroyTunnel(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).pcepDestroyTunnel(addLspInput);
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

}
