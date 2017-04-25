/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/*
* Generated file
*
* Generated from: yang module name: odl-data-change-counter-cfg yang module local name: data-change-counter-impl
* Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
* Generated at: Wed Aug 20 13:09:16 CEST 2014
*
* Do not modify this file unless it is present under src/main directory
*/
/**
 * @deprecated Replaced by blueprint wiring but remains for backwards compatibility until downstream users
 *             of the provided config system service are converted to blueprint.
 */
package org.opendaylight.controller.config.yang.bgpcep.data.change.counter;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;

public class DataChangeCounterImplModuleFactory extends org.opendaylight.controller.config.yang.bgpcep.data.change.counter.AbstractDataChangeCounterImplModuleFactory {
    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver, final BundleContext bundleContext) {
        final DataChangeCounterImplModule module = (DataChangeCounterImplModule) super.createModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
                               final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {
        final DataChangeCounterImplModule module = (DataChangeCounterImplModule)  super.createModule(instanceName, dependencyResolver, old, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
