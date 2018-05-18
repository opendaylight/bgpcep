/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceLoaderRIBExtensionConsumerContext extends SimpleRIBExtensionProviderContext
        implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceLoaderRIBExtensionConsumerContext.class);
    private final ServiceLoader<RIBExtensionProviderActivator> loader;

    private ServiceLoaderRIBExtensionConsumerContext(final ServiceLoader<RIBExtensionProviderActivator> loader) {
        this.loader = requireNonNull(loader);

        for (RIBExtensionProviderActivator a : loader) {
            a.startRIBExtensionProvider(this);
        }
    }

    @VisibleForTesting
    static ServiceLoaderRIBExtensionConsumerContext createConsumerContext() {
        final ServiceLoader<RIBExtensionProviderActivator> loader =
                ServiceLoader.load(RIBExtensionProviderActivator.class);

        return new ServiceLoaderRIBExtensionConsumerContext(loader);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        for (RIBExtensionProviderActivator a : this.loader) {
            try {
                a.stopRIBExtensionProvider();
            } catch (RuntimeException e) {
                LOG.warn("Stopping activator {} failed", a, e);
            }
        }
    }
}
