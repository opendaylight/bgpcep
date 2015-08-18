/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;


import java.util.ServiceLoader;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;

public final class ServiceLoaderRSVPExtensionProviderContext {
    private ServiceLoaderRSVPExtensionProviderContext() {
        throw new UnsupportedOperationException();
    }

    public static RSVPExtensionProviderContext create() {
        final RSVPExtensionProviderContext ctx = new SimpleRSVPExtensionProviderContext();

        final ServiceLoader<RSVPExtensionProviderActivator> loader = ServiceLoader.load(RSVPExtensionProviderActivator
            .class);
        for (final RSVPExtensionProviderActivator a : loader) {
            a.start(ctx);
        }

        return ctx;
    }

    public static RSVPExtensionProviderContext getSingletonInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final RSVPExtensionProviderContext INSTANCE;

        static {
            try {
                INSTANCE = create();
            } catch (final Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Holder() {
        }
    }
}
