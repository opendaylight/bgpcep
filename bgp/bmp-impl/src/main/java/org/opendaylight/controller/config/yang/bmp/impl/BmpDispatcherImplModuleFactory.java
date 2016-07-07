/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring but remains for backwards compatibility until downstream users
 *             of the provided config system service are converted to blueprint.
 */
@Deprecated
public class BmpDispatcherImplModuleFactory extends AbstractBmpDispatcherImplModuleFactory {
    @Override
    public BmpDispatcherImplModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BmpDispatcherImplModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        BmpDispatcherImplModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule,
                oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public BmpDispatcherImplModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BundleContext bundleContext) {
        BmpDispatcherImplModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

}
