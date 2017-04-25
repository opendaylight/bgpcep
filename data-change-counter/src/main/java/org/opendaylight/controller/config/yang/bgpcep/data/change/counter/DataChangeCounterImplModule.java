/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgpcep.data.change.counter;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.protocol.data.change.counter.TopologyDataChangeCounterDeployer;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring but remains for backwards compatibility until downstream users
 *             of the provided config system service are converted to blueprint.
 */
public class DataChangeCounterImplModule extends org.opendaylight.controller.config.yang.bgpcep.data.change.counter.AbstractDataChangeCounterImplModule {

    private BundleContext bundleContext;

    public DataChangeCounterImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataChangeCounterImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final org.opendaylight.controller.config.yang.bgpcep.data.change.counter.DataChangeCounterImplModule oldModule,
            final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getCounterId(), "value is not set.", counterIdJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTopologyName(), "value is not set.", topologyNameJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<TopologyDataChangeCounterDeployer> deployerTracker =
            WaitingServiceTracker.create(TopologyDataChangeCounterDeployer.class, this.bundleContext);
        final TopologyDataChangeCounterDeployer deployer = deployerTracker
            .waitForService(WaitingServiceTracker.FIVE_MINUTES);
        final String counterId = getCounterId();
        deployer.chandleCounterChange(counterId, getTopologyName());
        return ()->{
            deployer.deleteCounterChange(counterId);
            deployerTracker.close();
        };
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
