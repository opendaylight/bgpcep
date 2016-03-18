/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.odl_bgp_benchmark_cfg;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.protocol.bgp.benchmark.app.AppPeerBenchmark;

public class AppPeerBenchmarkModule extends AbstractAppPeerBenchmarkModule {
    public AppPeerBenchmarkModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public AppPeerBenchmarkModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final AppPeerBenchmarkModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getAppRibId(), "value is not set.", appRibIdJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new AppPeerBenchmark(getBindingDataBrokerDependency(), getRpcRegistryDependency(), getAppRibId());
    }

}
