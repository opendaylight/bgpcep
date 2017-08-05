/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Method;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring but remains for backwards compatibility until downstream users
 *             of the provided config system service are converted to blueprint.
 */
@Deprecated
public class BmpDispatcherImplModule extends AbstractBmpDispatcherImplModule {
    private BundleContext bundleContext;

    public BmpDispatcherImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpDispatcherImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bmp.impl.BmpDispatcherImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        // The BmpDispatcher instance is created and advertised as an OSGi service via blueprint
        // so obtain it here (waiting if necessary).
        final WaitingServiceTracker<BmpDispatcher> tracker =
                WaitingServiceTracker.create(BmpDispatcher.class, this.bundleContext);
        final BmpDispatcher service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        // Create a proxy to override close to close the ServiceTracker. The actual BmpDispatcher
        // instance will be closed via blueprint.
        return Reflection.newProxy(BmpDispatcher.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().equals("close")) {
                    tracker.close();
                    return null;
                }

                return method.invoke(service, args);
            }
        });
    }

    void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
