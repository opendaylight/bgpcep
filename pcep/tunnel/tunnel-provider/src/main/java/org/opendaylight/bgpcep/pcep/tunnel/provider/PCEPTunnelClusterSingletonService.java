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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.framework.BundleContext;
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

    @GuardedBy("this")
    private ServiceRegistration<?> serviceRegistration;
    @GuardedBy("this")
    private Registration pcepTunnelCssReg;
    @GuardedBy("this")
    private Registration reg;

    public PCEPTunnelClusterSingletonService(
            final TunnelProviderDependencies dependencies,
            final WithKey<Topology, TopologyKey> pcepTopology,
            final TopologyId tunnelTopologyId
    ) {
        this.tunnelTopologyId = requireNonNull(tunnelTopologyId);
        final TopologyId pcepTopologyId = pcepTopology.key().getTopologyId();
        final BundleContext bundleContext = dependencies.getBundleContext();

        final InstructionScheduler scheduler;
        ServiceTracker<InstructionScheduler, ?> tracker = null;
        try {
            tracker = new ServiceTracker<>(bundleContext,
                bundleContext.createFilter("(&(%s=%s)%s)".formatted(
                    Constants.OBJECTCLASS, InstructionScheduler.class.getName(),
                    "(" + InstructionScheduler.class.getName() + "=" + pcepTopologyId.getValue() + ")")),
                null);
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

        final var tunnelTopology = DataObjectIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(tunnelTopologyId))
                .build();
        ttp = new PCEPTunnelTopologyProvider(dependencies.getDataBroker(), pcepTopology, pcepTopologyId,
                tunnelTopology, tunnelTopologyId);


        sgi = scheduler.getIdentifier();
        tp = new TunnelProgramming(scheduler, dependencies);


        serviceRegistration = bundleContext.registerService(DefaultTopologyReference.class, ttp,
            FrameworkUtil.asDictionary(Map.of(
                PCEPTunnelTopologyProvider.class.getName(), tunnelTopologyId.getValue())));

        LOG.info("PCEP Tunnel Cluster Singleton service {} registered", getIdentifier().value());
        pcepTunnelCssReg = dependencies.getCssp().registerClusterSingletonService(this);
    }

    @Override
    public synchronized void instantiateServiceInstance() {
        LOG.info("Instantiate PCEP Tunnel Topology Provider Singleton Service {}", getIdentifier().value());

        reg = tp.register(DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(tunnelTopologyId))
            .build());
        ttp.init();
    }

    @Override
    public synchronized ListenableFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("Close Service Instance PCEP Tunnel Topology Provider Singleton Service {}", getIdentifier().value());
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
        LOG.info("Close PCEP Tunnel Topology Provider Singleton Service {}", getIdentifier().value());

        if (pcepTunnelCssReg != null) {
            try {
                pcepTunnelCssReg.close();
            } catch (final Exception e) {
                LOG.debug("Failed to close PCEP Tunnel Topology service {}", sgi.value(), e);
            }
            pcepTunnelCssReg = null;
        }
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
