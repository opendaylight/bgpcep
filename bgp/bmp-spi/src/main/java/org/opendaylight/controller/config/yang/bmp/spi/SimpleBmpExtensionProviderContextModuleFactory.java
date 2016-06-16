/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.spi;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class SimpleBmpExtensionProviderContextModuleFactory extends AbstractSimpleBmpExtensionProviderContextModuleFactory {
    @Override
    public SimpleBmpExtensionProviderContextModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            SimpleBmpExtensionProviderContextModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        SimpleBmpExtensionProviderContextModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule,
                oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public SimpleBmpExtensionProviderContextModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BundleContext bundleContext) {
        SimpleBmpExtensionProviderContextModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
