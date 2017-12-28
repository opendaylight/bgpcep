/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleBGPTableTypeRegistryProviderActivator implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleBGPTableTypeRegistryProviderActivator.class);

    private final BGPTableTypeRegistryProvider providerContext;
    private final List<BGPTableTypeRegistryProviderActivator> extensionActivators;

    public SimpleBGPTableTypeRegistryProviderActivator(final BGPTableTypeRegistryProvider providerContext,
            final List<BGPTableTypeRegistryProviderActivator> extensionActivators) {
        this.providerContext = requireNonNull(providerContext);
        this.extensionActivators = requireNonNull(extensionActivators);
    }

    public void start() {
        LOG.info("Starting {} BGPTableTypeRegistryProviderActivator instances", this.extensionActivators.size());

        this.extensionActivators.forEach(activator
            -> activator.startBGPTableTypeRegistryProvider(this.providerContext));
    }

    @Override
    public void close() {
        LOG.info("Stopping {} BGPTableTypeRegistryProviderActivator instances", this.extensionActivators.size());

        this.extensionActivators.forEach(BGPTableTypeRegistryProviderActivator::stopBGPTableTypeRegistryProvider);
    }

}
