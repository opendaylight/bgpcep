/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import java.util.ServiceLoader;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;

public final class ServiceLoaderBGPExtensionProviderContext {
    private static final class Holder {
        private static final BGPExtensionProviderContext INSTANCE = create();

        private Holder() {
        }
    }

    public static BGPExtensionProviderContext create() {
        final BGPExtensionProviderContext ctx = new SimpleBGPExtensionProviderContext();

        final ServiceLoader<BGPExtensionProviderActivator> loader = ServiceLoader.load(
            BGPExtensionProviderActivator.class);
        for (final BGPExtensionProviderActivator a : loader) {
            a.start(ctx);
        }

        return ctx;
    }

    public static BGPExtensionProviderContext getSingletonInstance() {
        return Holder.INSTANCE;
    }

    private ServiceLoaderBGPExtensionProviderContext() {
        throw new UnsupportedOperationException();
    }
}
