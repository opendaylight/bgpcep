/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.rsvp.spi;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Method;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class SimpleRSVPExtensionProviderContextModule extends org.opendaylight.controller.config.yang.rsvp.spi.AbstractSimpleRSVPExtensionProviderContextModule {
    private BundleContext bundleContext;

    public SimpleRSVPExtensionProviderContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SimpleRSVPExtensionProviderContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.rsvp.spi.SimpleRSVPExtensionProviderContextModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<RSVPExtensionProviderContext> tracker =
                WaitingServiceTracker.create(RSVPExtensionProviderContext.class, bundleContext);
        final RSVPExtensionProviderContext service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        return Reflection.newProxy(AutoCloseableRSVPExtensionProviderContext.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("close")) {
                    tracker.close();
                    return null;
                } else {
                    return method.invoke(service, args);
                }
            }
        });
    }

    void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static interface AutoCloseableRSVPExtensionProviderContext extends RSVPExtensionProviderContext, AutoCloseable {
    }
}
