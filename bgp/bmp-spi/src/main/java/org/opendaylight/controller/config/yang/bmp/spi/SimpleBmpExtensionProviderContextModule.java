/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.spi;

import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionProviderActivator;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;

public class SimpleBmpExtensionProviderContextModule extends org.opendaylight.controller.config.yang.bmp.spi.AbstractSimpleBmpExtensionProviderContextModule {
    public SimpleBmpExtensionProviderContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SimpleBmpExtensionProviderContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bmp.spi.SimpleBmpExtensionProviderContextModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class SimpleBmpExtensionProviderContextAutoCloseable extends SimpleBmpExtensionProviderContext implements AutoCloseable {
            @Override
            public void close() {
                for (final BmpExtensionProviderActivator e : getExtensionDependency()) {
                    e.stop();
                }
            }
        }

        final SimpleBmpExtensionProviderContextAutoCloseable ret = new SimpleBmpExtensionProviderContextAutoCloseable();
        for (final BmpExtensionProviderActivator e : getExtensionDependency()) {
            e.start(ret);
        }
        return ret;
    }

}
