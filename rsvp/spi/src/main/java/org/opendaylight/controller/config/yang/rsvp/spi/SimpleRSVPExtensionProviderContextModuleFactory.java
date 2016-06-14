/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.rsvp.spi;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class SimpleRSVPExtensionProviderContextModuleFactory extends AbstractSimpleRSVPExtensionProviderContextModuleFactory {
    @Override
    public SimpleRSVPExtensionProviderContextModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            SimpleRSVPExtensionProviderContextModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        SimpleRSVPExtensionProviderContextModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule,
                oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public SimpleRSVPExtensionProviderContextModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BundleContext bundleContext) {
        SimpleRSVPExtensionProviderContextModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
