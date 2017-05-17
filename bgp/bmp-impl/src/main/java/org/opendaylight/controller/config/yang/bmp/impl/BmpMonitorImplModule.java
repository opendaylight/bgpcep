/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import io.netty.util.internal.PlatformDependent;
import java.security.AccessControlException;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.protocol.bmp.impl.api.BmpDeployer;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitoringStation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.server.config.ServerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.MonitorId;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpMonitorImplModule extends org.opendaylight.controller.config.yang.bmp.impl.AbstractBmpMonitorImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMonitorImplModule.class);

    private static final int PRIVILEGED_PORTS = 1024;

    private BundleContext bundleContext;

    public BmpMonitorImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpMonitorImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bmp.impl.BmpMonitorImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getBindingPort(), bindingPortJmxAttribute);
        // check if unix root user
        if (!PlatformDependent.isWindows() && !PlatformDependent.isRoot() && getBindingPort().getValue() < PRIVILEGED_PORTS) {
            throw new AccessControlException("Unable to bind port " + getBindingPort().getValue() + " while running as non-root user.");
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<BmpDeployer> bgpDeployerTracker = WaitingServiceTracker
            .create(BmpDeployer.class, this.bundleContext);
        final BmpDeployer bgpDeployer = bgpDeployerTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.
            bmp.monitors.bmp.monitor.config.MonitoredRouter> monitoredRouters = convertBackwarCSS(getMonitoredRouter());

        final BmpMonitorConfig monitorConfig = new BmpMonitorConfigBuilder()
            .setServer(new ServerBuilder()
                .setBindingAddress(getBindingAddress())
                .setBindingPort(getBindingPort()).build())
            .setMonitorId(new MonitorId(getIdentifier().getInstanceName()))
            .setMonitoredRouter(monitoredRouters)
            .build();
        try {
            bgpDeployer.writeBmpMonitor(monitorConfig);
        } catch (final TransactionCommitFailedException e) {
            LOG.error("Failed to create BMP Monitor {}.", monitorConfig, e);
        }

        return (BmpMonitoringStation) () -> {
            bgpDeployer.deleteBmpMonitor(monitorConfig.getMonitorId());
            bgpDeployerTracker.close();
        };
    }

    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.
        bmp.monitors.bmp.monitor.config.MonitoredRouter> convertBackwarCSS(final List<MonitoredRouter> monitoredRouter) {
        return monitoredRouter.stream().map(this::convert).collect(Collectors.toList());
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.
        bmp.monitors.bmp.monitor.config.MonitoredRouter convert(final MonitoredRouter mr) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.
            bmp.monitors.bmp.monitor.config.MonitoredRouterBuilder().setActive(mr.getActive())
            .setAddress(mr.getAddress()).setPort(mr.getPort()).setPassword(mr.getPassword()).build();
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
