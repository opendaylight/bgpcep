/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.rsvp.spi;

import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;

public class SimpleRSVPExtensionProviderContextModule extends org.opendaylight.controller.config.yang.rsvp.spi.AbstractSimpleRSVPExtensionProviderContextModule {
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
        final class SimpleRSVPExtensionProviderContextAutoCloseable extends SimpleRSVPExtensionProviderContext implements AutoCloseable {
            @Override
            public void close() {
                for (final RSVPExtensionProviderActivator e : getRsvpExtensionDependency()) {
                    e.stop();
                }
            }
        }

        final SimpleRSVPExtensionProviderContextAutoCloseable ret = new SimpleRSVPExtensionProviderContextAutoCloseable();
        for (final RSVPExtensionProviderActivator e : getRsvpExtensionDependency()) {
            e.start(ret);
        }
        return ret;
    }

}
