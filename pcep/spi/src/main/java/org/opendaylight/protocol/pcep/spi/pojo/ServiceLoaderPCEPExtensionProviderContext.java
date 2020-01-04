/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import java.util.ServiceLoader;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;

public final class ServiceLoaderPCEPExtensionProviderContext extends SimplePCEPExtensionProviderContext {
    private static final class Holder {
        private static final PCEPExtensionProviderContext INSTANCE = create();

        private Holder() {
        }
    }

    private ServiceLoaderPCEPExtensionProviderContext() {
        // Hidden on purpose
    }

    public static PCEPExtensionProviderContext create() {
        final PCEPExtensionProviderContext ctx = new SimplePCEPExtensionProviderContext();

        final ServiceLoader<PCEPExtensionProviderActivator> loader =
                ServiceLoader.load(PCEPExtensionProviderActivator.class);
        for (final PCEPExtensionProviderActivator a : loader) {
            a.start(ctx);
        }

        return ctx;
    }

    public static PCEPExtensionProviderContext getSingletonInstance() {
        return Holder.INSTANCE;
    }
}
