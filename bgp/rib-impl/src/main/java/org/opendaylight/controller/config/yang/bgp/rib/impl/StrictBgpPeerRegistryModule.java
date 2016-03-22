/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Method;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.osgi.framework.BundleContext;

/**
 * Registry of BGP peers that allows only one connection per 2 peers
 *
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class StrictBgpPeerRegistryModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractStrictBgpPeerRegistryModule {
    private BundleContext bundleContext;

    public StrictBgpPeerRegistryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public StrictBgpPeerRegistryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.StrictBgpPeerRegistryModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<BGPPeerRegistry> tracker =
                WaitingServiceTracker.create(BGPPeerRegistry.class, this.bundleContext);
        final BGPPeerRegistry service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        return Reflection.newProxy(BGPPeerRegistry.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().equals("close")) {
                    tracker.close();
                    return null;
                } else {
                    return method.invoke(service, args);
                }
            }
        });
    }

    void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
