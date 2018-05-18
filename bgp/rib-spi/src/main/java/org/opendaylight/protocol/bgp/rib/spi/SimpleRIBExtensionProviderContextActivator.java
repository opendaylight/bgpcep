/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops RIBExtensionProviderActivator instances for a RIBExtensionProviderContext.
 */
public final class SimpleRIBExtensionProviderContextActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRIBExtensionProviderContextActivator.class);

    private final RIBExtensionProviderContext providerContext;
    private final List<RIBExtensionProviderActivator> extensionActivators;

    public SimpleRIBExtensionProviderContextActivator(final RIBExtensionProviderContext providerContext,
            final List<RIBExtensionProviderActivator> extensionActivators) {
        this.providerContext = requireNonNull(providerContext);
        this.extensionActivators = requireNonNull(extensionActivators);
    }

    public void start() {
        LOG.info("Starting {} RIBExtensionProviderActivator instances", this.extensionActivators.size());

        for (final RIBExtensionProviderActivator e : this.extensionActivators) {
            e.startRIBExtensionProvider(this.providerContext);
        }
    }

    @Override
    public void close() {
        LOG.info("Stopping {} RIBExtensionProviderActivator instances", this.extensionActivators.size());

        for (final RIBExtensionProviderActivator e : this.extensionActivators) {
            e.stopRIBExtensionProvider();
        }
    }
}
