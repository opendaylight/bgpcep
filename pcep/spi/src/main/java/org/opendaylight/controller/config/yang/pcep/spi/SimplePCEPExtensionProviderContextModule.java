/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: config-pcep-spi  yang module local name: pcep-extensions-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Mon Nov 18 14:32:53 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.pcep.spi;


/**
 *
 */
public final class SimplePCEPExtensionProviderContextModule extends
        org.opendaylight.controller.config.yang.pcep.spi.AbstractSimplePCEPExtensionProviderContextModule {

    public SimplePCEPExtensionProviderContextModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SimplePCEPExtensionProviderContextModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final SimplePCEPExtensionProviderContextModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public boolean canReuseInstance(final AbstractSimplePCEPExtensionProviderContextModule oldModule) {
        return oldModule.getInstance().getClass().equals(ReusablePCEPExtensionProviderContext.class);
    };

    @Override
    public java.lang.AutoCloseable reuseInstance(final java.lang.AutoCloseable oldInstance) {
        final ReusablePCEPExtensionProviderContext ctx = (ReusablePCEPExtensionProviderContext) oldInstance;
        ctx.reconfigure(getExtensionDependency());
        return ctx;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ReusablePCEPExtensionProviderContext ctx = new ReusablePCEPExtensionProviderContext();
        ctx.start(getExtensionDependency());
        return ctx;
    }
}
