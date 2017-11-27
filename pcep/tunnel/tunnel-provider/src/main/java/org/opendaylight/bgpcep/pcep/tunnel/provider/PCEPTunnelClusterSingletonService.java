/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.annotation.Nonnull;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTunnelClusterSingletonService implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTunnelClusterSingletonService.class);
    private final PCEPTunnelTopologyProvider ttp;
    private final TunnelProgramming tp;
    private final ServiceGroupIdentifier sgi;
    private final TopologyId tunnelTopology;
    private final TunnelProviderDependencies dependencies;
    private ServiceRegistration<?> serviceRegistration;
    private ClusterSingletonServiceRegistration pcepTunnelCssReg;
    private BindingAwareBroker.RoutedRpcRegistration<TopologyTunnelPcepProgrammingService> reg;

    public PCEPTunnelClusterSingletonService(
            final TunnelProviderDependencies dependencies,
            final InstanceIdentifier<Topology> pcepTopology,
            final TopologyId tunnelTopology
    ) {
        this.dependencies = requireNonNull(dependencies);
        this.tunnelTopology = requireNonNull(tunnelTopology);
        final TopologyId pcepTopologyId = pcepTopology.firstKeyOf(Topology.class).getTopologyId();
        final InstanceIdentifier<Topology> tunneltopology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(tunnelTopology)).build();
        this.ttp = new PCEPTunnelTopologyProvider(dependencies.getDataBroker(), pcepTopology, pcepTopologyId,
                tunneltopology);

        final WaitingServiceTracker<InstructionScheduler> schedulerTracker =
                WaitingServiceTracker.create(InstructionScheduler.class,
                        dependencies.getBundleContext(), "(" + InstructionScheduler.class.getName()
                                + "=" + pcepTopologyId.getValue() + ")");
        final InstructionScheduler scheduler = schedulerTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        schedulerTracker.close();
        this.sgi = scheduler.getIdentifier();
        this.tp = new TunnelProgramming(scheduler, dependencies);


        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(PCEPTunnelTopologyProvider.class.getName(), tunnelTopology.getValue());
        this.serviceRegistration = dependencies.getBundleContext()
                .registerService(DefaultTopologyReference.class.getName(), this.ttp, properties);

        LOG.info("PCEP Tunnel Cluster Singleton service {} registered", getIdentifier().getValue());
        this.pcepTunnelCssReg = dependencies.getCssp().registerClusterSingletonService(this);
    }


    @Override
    public void instantiateServiceInstance() {
        this.reg = this.dependencies.getRpcProviderRegistry()
                .addRoutedRpcImplementation(TopologyTunnelPcepProgrammingService.class, this.tp);

        final InstanceIdentifier<Topology> topology = InstanceIdentifier
                .builder(NetworkTopology.class).child(Topology.class, new TopologyKey(this.tunnelTopology)).build();
        this.reg.registerPath(NetworkTopologyContext.class, topology);
        this.ttp.init();
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        this.reg.close();
        this.tp.close();
        this.ttp.close();
        return Futures.immediateFuture(null);
    }

    @Nonnull
    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return this.sgi;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        if (this.pcepTunnelCssReg != null) {
            try {
                this.pcepTunnelCssReg.close();
            } catch (final Exception e) {
                LOG.debug("Failed to close PCEP Tunnel Topology service {}", this.sgi.getValue(), e);
            }
            this.pcepTunnelCssReg = null;
        }
        if (this.serviceRegistration != null) {
            this.serviceRegistration.unregister();
            this.serviceRegistration = null;
        }
    }
}
