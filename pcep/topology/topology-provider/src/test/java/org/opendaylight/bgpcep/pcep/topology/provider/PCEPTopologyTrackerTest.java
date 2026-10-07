/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectWritten;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.topology.stats.rpc.rev190321.GetStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev260528.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev260528.network.topology.topology.topology.types.TopologyPcep;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCEPTopologyTrackerTest {
    private static final String TOPOLOGY_ID = "pcep-topology";
    private static final TopologyKey TOPOLOGY_KEY = new TopologyKey(new TopologyId(TOPOLOGY_ID));
    private static final DataObjectIdentifier<TopologyPcep> TOPOLOGY_PCEP_ID =
        DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, TOPOLOGY_KEY)
            .child(TopologyTypes.class)
            .augmentation(TopologyTypes1.class)
            .child(TopologyPcep.class)
            .build();
    private static final DataObjectIdentifier<Topology> TOPOLOGY_INSTANCE_ID =
        DataObjectIdentifier.builder(NetworkTopology.class).child(Topology.class, TOPOLOGY_KEY).build();

    private enum Delivery {
        // The initial notification is not delivered, the test drives the listener by itself
        NONE,
        // The initial notification is delivered synchronously on the registering thread
        ON_REGISTERING_THREAD,
        // The initial notification is delivered from another thread before the registration returns
        BEFORE_REGISTRATION_RETURNS
    }

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ClusterSingletonServiceProvider singletonService;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private PCEPExtensionConsumerContext extensions;
    @Mock
    private PCEPDispatcher dispatcher;
    @Mock
    private InstructionSchedulerFactory instructionSchedulerFactory;
    @Mock
    private PceServerProvider pceServerProvider;
    @Mock
    private Registration statsListenerReg;
    @Mock
    private Registration statsRpcReg;
    @Mock
    private InstructionScheduler scheduler;
    @Mock
    private Registration singletonReg;

    private PCEPTopologyTracker tracker;
    private Delivery delivery;
    private DataTreeChangeListener<TopologyPcep> topologyListener;

    @Before
    public void setUp() {
        // Registrations made by the tracker constructor: the stats listener in OPERATIONAL, the topology listener in
        // CONFIGURATION
        doAnswer(inv -> {
            if (inv.getArgument(0) == LogicalDatastoreType.CONFIGURATION) {
                return registerTopologyListener(inv.getArgument(2));
            }
            return statsListenerReg;
        }).when(dataBroker).registerTreeChangeListener(any(LogicalDatastoreType.class),
            any(DataObjectReference.class), any());
        doReturn(statsRpcReg).when(rpcProviderService).registerRpcImplementation(any(GetStats.class));

        // Topology singleton created for each configured PCEP topology
        doReturn(scheduler).when(instructionSchedulerFactory).createInstructionScheduler(TOPOLOGY_ID);
        doReturn(singletonReg).when(singletonService).registerClusterSingletonService(
            any(ClusterSingletonService.class));
    }

    @After
    public void tearDown() {
        if (tracker != null) {
            tracker.close();
        }
    }

    /**
     * Reproducer for the PCEP topology instance not being created on startup.
     *
     * <p>The distributed datastore registers a listener asynchronously and delivers the initial data on a
     * notification thread, which may happen before {@code registerTreeChangeListener()} returns. That initial
     * notification must result in the topology instance being created.
     */
    @Test
    public void testInitialNotificationDeliveredBeforeRegistrationReturns() {
        delivery = Delivery.BEFORE_REGISTRATION_RETURNS;
        tracker = newTracker();

        verify(instructionSchedulerFactory, timeout(5000)).createInstructionScheduler(TOPOLOGY_ID);
        verify(singletonService, timeout(5000)).registerClusterSingletonService(any(ClusterSingletonService.class));
    }

    /**
     * Same as {@link #testInitialNotificationDeliveredBeforeRegistrationReturns()}, but the initial notification is
     * delivered synchronously on the registering thread, as a datastore with a direct notification executor does.
     */
    @Test
    public void testInitialNotificationDeliveredOnRegisteringThread() {
        delivery = Delivery.ON_REGISTERING_THREAD;
        tracker = newTracker();

        verify(instructionSchedulerFactory).createInstructionScheduler(TOPOLOGY_ID);
        verify(singletonService).registerClusterSingletonService(any(ClusterSingletonService.class));
    }

    /**
     * Notifications delivered after the tracker has been closed must not create any topology instance.
     */
    @Test
    public void testNotificationAfterCloseIsIgnored() {
        delivery = Delivery.NONE;
        tracker = newTracker();
        tracker.close();

        topologyListener.onDataTreeChanged(List.of(initialModification()));

        verifyNoInteractions(instructionSchedulerFactory, singletonService);
    }

    /**
     * Reproducer for the PCEP topology instance not being instantiated on startup.
     *
     * <p>The cluster singleton service provider instantiates a service synchronously from
     * {@code registerClusterSingletonService()} when the service group is already owned, which happens when the
     * instruction scheduler sharing that group has already been granted ownership. That instantiation must result in
     * the topology instance being created.
     */
    @Test
    public void testSingletonInstantiatedBeforeRegistrationReturns() {
        doAnswer(inv -> {
            final ClusterSingletonService service = inv.getArgument(0);
            service.instantiateServiceInstance();
            final var reg = mock(Registration.class);
            doAnswer(closeInv -> service.closeServiceInstance()).when(reg).close();
            return reg;
        }).when(singletonService).registerClusterSingletonService(any(ClusterSingletonService.class));
        doReturn(mock(Registration.class)).when(dataBroker).registerDataListener(
            eq(LogicalDatastoreType.CONFIGURATION), eq(TOPOLOGY_INSTANCE_ID), any());

        delivery = Delivery.ON_REGISTERING_THREAD;
        tracker = newTracker();

        // PCEPTopologyInstance subscribes to its topology configuration when it is created
        verify(dataBroker).registerDataListener(eq(LogicalDatastoreType.CONFIGURATION), eq(TOPOLOGY_INSTANCE_ID),
            any());
    }

    private PCEPTopologyTracker newTracker() {
        return new PCEPTopologyTracker(dataBroker, singletonService, rpcProviderService, extensions, dispatcher,
            instructionSchedulerFactory, pceServerProvider);
    }

    private Registration registerTopologyListener(final DataTreeChangeListener<TopologyPcep> listener)
            throws Exception {
        topologyListener = listener;
        switch (delivery) {
            case NONE -> {
                // Nothing to do
            }
            case ON_REGISTERING_THREAD -> listener.onDataTreeChanged(List.of(initialModification()));
            case BEFORE_REGISTRATION_RETURNS -> {
                // Deliver the initial notification from another thread, as the datastore does, and give it a chance
                // to complete before the registration is handed back to the caller
                final var future = CompletableFuture.runAsync(
                    () -> listener.onDataTreeChanged(List.of(initialModification())));
                try {
                    future.get(1, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    // The delivery has not completed yet, the verification below waits for it
                }
            }
            default -> throw new AssertionError("Unhandled delivery " + delivery);
        }
        return mock(Registration.class);
    }

    private static DataTreeModification<TopologyPcep> initialModification() {
        // A newly-written topology, i.e. one without data before
        final var rootNode = mock(DataObjectWritten.class);
        final var modification = mock(DataTreeModification.class);
        doReturn(rootNode).when(modification).getRootNode();
        doReturn(TOPOLOGY_PCEP_ID).when(modification).path();
        return modification;
    }
}
