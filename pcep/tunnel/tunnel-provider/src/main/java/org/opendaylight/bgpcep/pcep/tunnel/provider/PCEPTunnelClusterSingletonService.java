/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.TopologyTunnelPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTunnelClusterSingletonService implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTunnelClusterSingletonService.class);
    private final PCEPTunnelTopologyProvider ttp;
    private final TunnelProgramming tp;
    private final ServiceGroupIdentifier sgi;
    private final TopologyId tunnelTopologyId;
    private final TunnelProviderDependencies dependencies;
    @GuardedBy("this")
    private ServiceRegistration<?> serviceRegistration;
    @GuardedBy("this")
    private ClusterSingletonServiceRegistration pcepTunnelCssReg;
    @GuardedBy("this")
    private ObjectRegistration<TunnelProgramming> reg;

    public PCEPTunnelClusterSingletonService(
            final TunnelProviderDependencies dependencies,
            final InstanceIdentifier<Topology> pcepTopology,
            final TopologyId tunnelTopologyId
    ) {
        this.dependencies = requireNonNull(dependencies);
        this.tunnelTopologyId = requireNonNull(tunnelTopologyId);
        final TopologyId pcepTopologyId = pcepTopology.firstKeyOf(Topology.class).getTopologyId();

        final InstructionScheduler scheduler;
        ServiceTracker<InstructionScheduler, ?> tracker = null;
        try {
            tracker = new ServiceTracker<>(dependencies.getBundleContext(),
                    dependencies.getBundleContext().createFilter(String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS,
                            InstructionScheduler.class.getName(), "(" + InstructionScheduler.class.getName()
                            + "=" + pcepTopologyId.getValue() + ")")), null);
            tracker.open();
            scheduler = (InstructionScheduler) tracker.waitForService(
                    TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
            Preconditions.checkState(scheduler != null, "InstructionScheduler service not found");
        } catch (InvalidSyntaxException | InterruptedException e) {
            throw new IllegalStateException("Error retrieving InstructionScheduler service", e);
        } finally {
            if (tracker != null) {
                tracker.close();
            }
        }

        final InstanceIdentifier<Topology> tunnelTopology = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(tunnelTopologyId)).build();
        ttp = new PCEPTunnelTopologyProvider(dependencies.getDataBroker(), pcepTopology, pcepTopologyId,
                tunnelTopology, tunnelTopologyId);


        sgi = scheduler.getIdentifier();
        tp = new TunnelProgramming(scheduler, dependencies);


        serviceRegistration = dependencies.getBundleContext()
                .registerService(DefaultTopologyReference.class.getName(), ttp, props(tunnelTopologyId));

        LOG.info("PCEP Tunnel Cluster Singleton service {} registered", getIdentifier().getName());
        pcepTunnelCssReg = dependencies.getCssp().registerClusterSingletonService(this);
    }

    private static Dictionary<String, String> props(final TopologyId tunnelTopologyId) {
        return FrameworkUtil.asDictionary(Map.of(
            PCEPTunnelTopologyProvider.class.getName(), tunnelTopologyId.getValue()));
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.info("Instantiate PCEP Tunnel Topology Provider Singleton Service {}", getIdentifier().getName());

        final InstanceIdentifier<Topology> topology = InstanceIdentifier
                .builder(NetworkTopology.class).child(Topology.class, new TopologyKey(tunnelTopologyId)).build();
        reg = dependencies.getRpcProviderRegistry()
                .registerRpcImplementation(TopologyTunnelPcepProgrammingService.class, tp, Set.of(topology));
        ttp.init();
    }

    @Override
    public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("Close Service Instance PCEP Tunnel Topology Provider Singleton Service {}",
                getIdentifier().getName());
        reg.close();
        tp.close();
        ttp.close();
        return CommitInfo.emptyFluentFuture();
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return sgi;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public synchronized void close() {
        LOG.info("Close PCEP Tunnel Topology Provider Singleton Service {}", getIdentifier().getName());

        if (pcepTunnelCssReg != null) {
            try {
                pcepTunnelCssReg.close();
            } catch (final Exception e) {
                LOG.debug("Failed to close PCEP Tunnel Topology service {}", sgi.getName(), e);
            }
            pcepTunnelCssReg = null;
        }
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
