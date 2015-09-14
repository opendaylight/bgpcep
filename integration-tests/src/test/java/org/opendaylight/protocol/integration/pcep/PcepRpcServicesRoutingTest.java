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
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitAddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitEnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitRemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerInitialSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerInitialSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerReSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerReSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerReSyncLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitTriggerReSyncLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev131106.SubmitUpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerInitialSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerInitialSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerInitialSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerReSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerReSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerReSyncLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerReSyncLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerReSyncLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerReSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;

@RunWith(PaxExam.class)
@ExamReactorStrategy(org.ops4j.pax.exam.spi.reactors.PerClass.class)
public class PcepRpcServicesRoutingTest extends AbstractPcepOsgiTest {

    @Test
    public void testRoutedRpcNetworkTopologyPcepService() throws Exception {
        final NetworkTopologyPcepService pcepService1 = mock(NetworkTopologyPcepService.class, "First pcep Service");
        final NetworkTopologyPcepService pcepService2 = mock(NetworkTopologyPcepService.class, "Second pcep Service");
        initMock(pcepService1);
        initMock(pcepService2);
        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo1"))).build();

        final BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> firstReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(NetworkTopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo2"))).build();

        final BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepService> secondReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepService.class, pcepService2);
                secondReg.registerPath(NetworkTopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        final BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
                final NetworkTopologyPcepService consumerPcepService = session.getRpcService(NetworkTopologyPcepService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                testAddLspRpce(consumerPcepService);
                testEnsureLspRpce(consumerPcepService);
                testTriggerInitialSyncRpce(consumerPcepService);
                testTriggerReSyncRpce(consumerPcepService);
                testTriggerReSyncLspRpce(consumerPcepService);
            }

            private void testAddLspRpce(final NetworkTopologyPcepService consumerPcepService) {
                AddLspInput addLspInput = getInputForRpc(topology, AddLspInputBuilder.class, AddLspInput.class);
                consumerPcepService.addLsp(addLspInput);

                verify(pcepService1).addLsp(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getInputForRpc(topology2, AddLspInputBuilder.class, AddLspInput.class);

                consumerPcepService.addLsp(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).addLsp(addLspInput);
            }

            private void testTriggerInitialSyncRpce(final NetworkTopologyPcepService consumerPcepService) {
                TriggerInitialSyncInput triggerInput = getInputForRpc(topology, TriggerInitialSyncInputBuilder.class,
                    TriggerInitialSyncInput.class);
                consumerPcepService.triggerInitialSync(triggerInput);

                verify(pcepService1).triggerInitialSync(triggerInput);
                verifyZeroInteractions(pcepService2);

                triggerInput = getInputForRpc(topology2, TriggerInitialSyncInputBuilder.class, TriggerInitialSyncInput.class);

                consumerPcepService.triggerInitialSync(triggerInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).triggerInitialSync(triggerInput);
            }

            private void testTriggerReSyncRpce(final NetworkTopologyPcepService consumerPcepService) {
                TriggerReSyncInput triggerInput = getInputForRpc(topology, TriggerReSyncInputBuilder.class,
                    TriggerReSyncInput.class);
                consumerPcepService.triggerReSync(triggerInput);

                verify(pcepService1).triggerReSync(triggerInput);
                verifyZeroInteractions(pcepService2);

                triggerInput = getInputForRpc(topology2, TriggerReSyncInputBuilder.class, TriggerReSyncInput.class);

                consumerPcepService.triggerReSync(triggerInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).triggerReSync(triggerInput);
            }

            private void testTriggerReSyncLspRpce(final NetworkTopologyPcepService consumerPcepService) {
                TriggerReSyncLspInput triggerInput = getInputForRpc(topology, TriggerReSyncLspInputBuilder.class,
                    TriggerReSyncLspInput.class);
                consumerPcepService.triggerReSyncLsp(triggerInput);

                verify(pcepService1).triggerReSyncLsp(triggerInput);
                verifyZeroInteractions(pcepService2);

                triggerInput = getInputForRpc(topology2, TriggerReSyncLspInputBuilder.class, TriggerReSyncLspInput
                    .class);

                consumerPcepService.triggerReSyncLsp(triggerInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).triggerReSyncLsp(triggerInput);
            }

            private void testEnsureLspRpce(final NetworkTopologyPcepService consumerPcepService) {
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

    @SuppressWarnings("unchecked")
    private void initMock(final NetworkTopologyPcepService pcepService) {

        @SuppressWarnings("rawtypes")
        final ListenableFuture future = Futures.immediateFuture(RpcResultBuilder.<AddLspOutput>success().build());
        final ListenableFuture futureTrigger = Futures.immediateFuture(RpcResultBuilder.<TriggerInitialSyncOutput>success().build());
        final ListenableFuture futureReSyncTrigger = Futures.immediateFuture(RpcResultBuilder
            .<TriggerReSyncOutput>success().build());
        final ListenableFuture futureReSyncLspTrigger = Futures.immediateFuture(RpcResultBuilder
            .<TriggerReSyncLspOutput>success().build());

        when(pcepService.addLsp(Mockito.<AddLspInput>any())).thenReturn(future);
        when(pcepService.removeLsp(Mockito.<RemoveLspInput>any())).thenReturn(future);
        when(pcepService.triggerInitialSync(Mockito.<TriggerInitialSyncInput>any())).thenReturn(futureTrigger);
        when(pcepService.triggerReSync(Mockito.<TriggerReSyncInput>any())).thenReturn(futureReSyncTrigger);
        when(pcepService.triggerReSyncLsp(Mockito.<TriggerReSyncLspInput>any())).thenReturn(futureReSyncLspTrigger);
        when(pcepService.ensureLspOperational(Mockito.<EnsureLspOperationalInput>any())).thenReturn(future);
        when(pcepService.updateLsp(Mockito.<UpdateLspInput>any())).thenReturn(future);

    }

    private static <T> T getInputForRpc(final InstanceIdentifier<Topology> topology, final Class<?> builderClass, final Class<T> builtObjectClass) {
        try {
            final Object builderInstance = builderClass.newInstance();
            final Method method = builderClass.getMethod("setNetworkTopologyRef", NetworkTopologyRef.class);
            method.invoke(builderInstance, new NetworkTopologyRef(topology));
            return builtObjectClass.cast(builderClass.getMethod("build").invoke(builderInstance));
        } catch (final Exception e) {
            throw new RuntimeException("Unable to create instance from " + builderClass, e);
        }
    }

    @Test
    public void testRoutedRpcNetworkTopologyPcepProgrammingService() throws Exception {
        final NetworkTopologyPcepProgrammingService pcepService1 = mock(NetworkTopologyPcepProgrammingService.class,
                "First pcep program Service");
        final NetworkTopologyPcepProgrammingService pcepService2 = mock(NetworkTopologyPcepProgrammingService.class,
                "Second pcep program Service");
        initMock(pcepService1);
        initMock(pcepService2);
        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo1"))).build();

        final BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> firstReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepProgrammingService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(NetworkTopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo2"))).build();

        final BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                final BindingAwareBroker.RoutedRpcRegistration<NetworkTopologyPcepProgrammingService> secondReg = session.addRoutedRpcImplementation(
                        NetworkTopologyPcepProgrammingService.class, pcepService2);
                secondReg.registerPath(NetworkTopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        final BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
                final NetworkTopologyPcepProgrammingService consumerPcepService = session.getRpcService(NetworkTopologyPcepProgrammingService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                testSubmitAddLspRpc(consumerPcepService);
                testSubmitUpdateLspRpc(consumerPcepService);
                testTriggerInitialSyncRpc(consumerPcepService);
                testTriggerReSyncRpc(consumerPcepService);
                testTriggerReSyncLspRpc(consumerPcepService);
            }

            private void testSubmitAddLspRpc(final NetworkTopologyPcepProgrammingService consumerPcepService) {
                SubmitAddLspInput addLspInput = getInputForRpc(topology, SubmitAddLspInputBuilder.class, SubmitAddLspInput.class);
                consumerPcepService.submitAddLsp(addLspInput);

                verify(pcepService1).submitAddLsp(addLspInput);
                verifyZeroInteractions(pcepService2);

                addLspInput = getInputForRpc(topology2, SubmitAddLspInputBuilder.class, SubmitAddLspInput.class);

                consumerPcepService.submitAddLsp(addLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitAddLsp(addLspInput);
            }

            private void testSubmitUpdateLspRpc(final NetworkTopologyPcepProgrammingService consumerPcepService) {
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

            private void testTriggerInitialSyncRpc(final NetworkTopologyPcepProgrammingService consumerPcepService) {
                SubmitTriggerInitialSyncInput submitTriggerInput = getInputForRpc(topology,
                    SubmitTriggerInitialSyncInputBuilder.class,
                    SubmitTriggerInitialSyncInput.class);
                consumerPcepService.submitTriggerInitialSync(submitTriggerInput);

                verify(pcepService1).submitTriggerInitialSync(submitTriggerInput);
                verifyZeroInteractions(pcepService2);

                submitTriggerInput = getInputForRpc(topology2, SubmitTriggerInitialSyncInputBuilder.class,
                    SubmitTriggerInitialSyncInput.class);

                consumerPcepService.submitTriggerInitialSync(submitTriggerInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitTriggerInitialSync(submitTriggerInput);
            }

            private void testTriggerReSyncRpc(final NetworkTopologyPcepProgrammingService consumerPcepService) {
                SubmitTriggerReSyncInput submitTriggerReSyncInput = getInputForRpc(topology,
                    SubmitTriggerReSyncInputBuilder.class,
                    SubmitTriggerReSyncInput.class);
                consumerPcepService.submitTriggerReSync(submitTriggerReSyncInput);

                verify(pcepService1).submitTriggerReSync(submitTriggerReSyncInput);
                verifyZeroInteractions(pcepService2);

                submitTriggerReSyncInput = getInputForRpc(topology2, SubmitTriggerReSyncInputBuilder.class,
                    SubmitTriggerReSyncInput.class);

                consumerPcepService.submitTriggerReSync(submitTriggerReSyncInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitTriggerReSync(submitTriggerReSyncInput);
            }

            private void testTriggerReSyncLspRpc(final NetworkTopologyPcepProgrammingService consumerPcepService) {
                SubmitTriggerReSyncLspInput submitTriggerReSyncLspInput = getInputForRpc(topology,
                    SubmitTriggerReSyncLspInputBuilder.class,
                    SubmitTriggerReSyncLspInput.class);
                consumerPcepService.submitTriggerReSyncLsp(submitTriggerReSyncLspInput);

                verify(pcepService1).submitTriggerReSyncLsp(submitTriggerReSyncLspInput);
                verifyZeroInteractions(pcepService2);

                submitTriggerReSyncLspInput = getInputForRpc(topology2, SubmitTriggerReSyncLspInputBuilder.class,
                    SubmitTriggerReSyncLspInput.class);

                consumerPcepService.submitTriggerReSyncLsp(submitTriggerReSyncLspInput);

                verifyZeroInteractions(pcepService1);
                verify(pcepService2).submitTriggerReSyncLsp(submitTriggerReSyncLspInput);
            }
        };
        broker.registerConsumer(consumer, getBundleContext());
    }

    @SuppressWarnings("unchecked")
    private void initMock(final NetworkTopologyPcepProgrammingService pcepService) {
        @SuppressWarnings("rawtypes")
        final ListenableFuture future = Futures.immediateFuture(RpcResultBuilder.<AddLspOutput>success().build());
        final ListenableFuture futureTrigger = Futures.immediateFuture(RpcResultBuilder.<TriggerInitialSyncOutput>success().build
            ());
        when(pcepService.submitAddLsp(Mockito.<SubmitAddLspInput>any())).thenReturn(future);
        when(pcepService.submitRemoveLsp(Mockito.<SubmitRemoveLspInput>any())).thenReturn(future);
        when(pcepService.submitTriggerInitialSync(Mockito.<SubmitTriggerInitialSyncInput>any())).thenReturn(futureTrigger);
        when(pcepService.submitEnsureLspOperational(Mockito.<SubmitEnsureLspOperationalInput>any())).thenReturn(future);
        when(pcepService.submitUpdateLsp(Mockito.<SubmitUpdateLspInput>any())).thenReturn(future);
    }

    @Test
    public void testRoutedRpcTopologyTunnelPcepProgrammingService() throws Exception {
        final TopologyTunnelPcepProgrammingService pcepService1 = mock(TopologyTunnelPcepProgrammingService.class,
                "First pcep program Service");
        final TopologyTunnelPcepProgrammingService pcepService2 = mock(TopologyTunnelPcepProgrammingService.class,
                "Second pcep program Service");
        initMock(pcepService1);
        initMock(pcepService2);
        assertNotNull(getBroker());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo1"))).build();

        final BindingAwareProvider provider1 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                final BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> firstReg = session.addRoutedRpcImplementation(
                        TopologyTunnelPcepProgrammingService.class, pcepService1);
                assertNotNull("Registration should not be null", firstReg);
                assertSame(pcepService1, firstReg.getInstance());

                firstReg.registerPath(NetworkTopologyContext.class, topology);
            }
        };

        broker.registerProvider(provider1, getBundleContext());

        final InstanceIdentifier<Topology> topology2 = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId("Topo2"))).build();

        final BindingAwareProvider provider2 = new AbstractTestProvider() {

            @Override
            public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
                assertNotNull(session);
                final BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> secondReg = session.addRoutedRpcImplementation(
                        TopologyTunnelPcepProgrammingService.class, pcepService2);
                secondReg.registerPath(NetworkTopologyContext.class, topology2);
            }
        };

        broker.registerProvider(provider2, getBundleContext());

        final BindingAwareConsumer consumer = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(final BindingAwareBroker.ConsumerContext session) {
                final TopologyTunnelPcepProgrammingService consumerPcepService = session.getRpcService(TopologyTunnelPcepProgrammingService.class);

                assertNotNull(consumerPcepService);
                assertNotSame(pcepService1, consumerPcepService);
                assertNotSame(pcepService2, consumerPcepService);

                testCreateP2pTunnel(consumerPcepService);
                testDestroyP2pTunnel(consumerPcepService);
            }

            private void testCreateP2pTunnel(final TopologyTunnelPcepProgrammingService consumerPcepService) {
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

            private void testDestroyP2pTunnel(final TopologyTunnelPcepProgrammingService consumerPcepService) {
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

    private void initMock(final TopologyTunnelPcepProgrammingService pcepService) {

        @SuppressWarnings("rawtypes")
        final ListenableFuture future = Futures.immediateFuture(RpcResultBuilder.<AddLspOutput>success().build());
        when(pcepService.pcepCreateP2pTunnel(Mockito.<PcepCreateP2pTunnelInput>any())).thenReturn(future);
        when(pcepService.pcepDestroyTunnel(Mockito.<PcepDestroyTunnelInput>any())).thenReturn(future);
        when(pcepService.pcepUpdateTunnel(Mockito.<PcepUpdateTunnelInput>any())).thenReturn(future);
    }

}
