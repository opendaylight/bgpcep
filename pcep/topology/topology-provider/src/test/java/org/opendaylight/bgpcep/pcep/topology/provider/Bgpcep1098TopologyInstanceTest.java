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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.pcep.config.SessionConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev260528.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev260528.network.topology.topology.topology.types.TopologyPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * BGPCEP-1098 tests to prove {@link PCEPTopologyInstance}'s ability to handle notification over its full lifecycle.
 *
 * <p>We have to verify that very first datastore notification is processed just right when listener is registered and
 * that the datastore notification coming after instance is closed is being skipped.
 */
@ExtendWith(MockitoExtension.class)
@Timeout(10)
class Bgpcep1098TopologyInstanceTest {
    private static final DataObjectIdentifier.WithKey<Topology, TopologyKey> TOPOLOGY_PATH =
        DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("pcep-topology")))
            .build();

    @Mock
    private DataBroker dataBroker;
    @Mock
    private PCEPTopologyProviderDependencies dependencies;
    @Mock
    private InstructionScheduler scheduler;
    @Mock
    private WriteTransaction tx;
    @Mock
    private Registration noopRegistration;

    @BeforeEach
    void setUp() {
        doReturn(dataBroker).when(dependencies).getDataBroker();
    }

    /**
     * Stub the operational topology creation performed by {@link PCEPTopologyProvider} once it receives its
     * configuration. The commit never completes, hence the PCEP server channel is never started.
     */
    private void stubTopologyCreation() {
        doReturn(tx).when(dataBroker).newWriteOnlyTransaction();
        doNothing().when(tx).put(eq(LogicalDatastoreType.OPERATIONAL), any(DataObjectIdentifier.class),
            any(Topology.class));
        doReturn(FluentFuture.from(SettableFuture.<CommitInfo>create())).when(tx).commit();
        doReturn(null).when(dependencies).getPceServerProvider();
    }

    /**
     * Reproducer for the topology provider not picking up its configuration on startup.
     *
     * <p>The distributed datastore registers a listener asynchronously and delivers the initial data on a
     * notification thread, which may happen before {@code registerDataListener()} returns. Since the topology
     * configuration is written only once, that initial notification must reach the {@link PCEPTopologyProvider},
     * otherwise the PCEP server is never started.
     */
    @Test
    void testInitialNotificationDeliveredBeforeRegistrationReturns() {
        stubTopologyCreation();
        doAnswer(inv -> {
            final DataListener<Topology> listener = inv.getArgument(2);
            // Deliver the initial notification from another thread, as the datastore does, and wait for it to
            // complete before the registration is handed back to the caller
            CompletableFuture.runAsync(() -> listener.dataChangedTo(configuration()))
                .orTimeout(5, TimeUnit.SECONDS)
                .join();
            return noopRegistration;
        }).when(dataBroker).registerDataListener(eq(LogicalDatastoreType.CONFIGURATION),
            any(DataObjectIdentifier.class), any());

        newTopologyInstance();

        // The provider must have been handed the configuration, which is when it creates the operational topology
        verify(dataBroker).newWriteOnlyTransaction();
    }

    /**
     * Synchronous version of {@link #testInitialNotificationDeliveredBeforeRegistrationReturns()}.
     */
    @Test
    void testInitialNotificationDeliveredOnRegisteringThread() {
        stubTopologyCreation();
        doAnswer(inv -> {
            final DataListener<Topology> listener = inv.getArgument(2);
            listener.dataChangedTo(configuration());
            return noopRegistration;
        }).when(dataBroker).registerDataListener(eq(LogicalDatastoreType.CONFIGURATION),
            any(DataObjectIdentifier.class), any());

        newTopologyInstance();

        verify(dataBroker).newWriteOnlyTransaction();
    }

    /**
     * Notifications delivered after the instance has been terminated must not reach the {@link PCEPTopologyProvider}.
     */
    @Test
    void testNotificationAfterTerminateIsIgnored() {
        doNothing().when(noopRegistration).close();

        final var listener = ArgumentCaptor.<DataListener<Topology>>captor();
        doReturn(noopRegistration).when(dataBroker).registerDataListener(eq(LogicalDatastoreType.CONFIGURATION),
            any(DataObjectIdentifier.class), listener.capture());

        newTopologyInstance().terminate();

        listener.getValue().dataChangedTo(configuration());

        verify(dataBroker, never()).newWriteOnlyTransaction();
    }

    private PCEPTopologyInstance newTopologyInstance() {
        return new PCEPTopologyInstance(dataBroker, new PCEPTopologyProvider(TOPOLOGY_PATH, dependencies, scheduler));
    }

    private static Topology configuration() {
        return new TopologyBuilder()
            .withKey(TOPOLOGY_PATH.key())
            .setTopologyTypes(new TopologyTypesBuilder()
                .addAugmentation(new TopologyTypes1Builder()
                    .setTopologyPcep(new TopologyPcepBuilder()
                        .setSessionConfig(new SessionConfigBuilder()
                            .setListenAddress(new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.1")))
                            .setListenPort(new PortNumber(Uint16.valueOf(4189)))
                            .setRpcTimeout((short) 30)
                            .setDeadTimerValue(Uint8.valueOf(120))
                            .setKeepAliveTimerValue(Uint8.valueOf(30))
                            .setMaxUnknownMessages(Uint16.valueOf(5))
                            .build())
                        .build())
                    .build())
                .build())
            .build();
    }
}
