/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import com.google.common.base.Preconditions;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops BmpExtensionProviderActivator instances for a BmpExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
public class SimpleBmpExtensionProviderContextActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleBmpExtensionProviderContextActivator.class);

    private final BmpExtensionProviderContext providerContext;
    private final List<BmpExtensionProviderActivator> extensionActivators;

    public SimpleBmpExtensionProviderContextActivator(final BmpExtensionProviderContext providerContext,
            final List<BmpExtensionProviderActivator> extensionActivators) {
        this.providerContext = Preconditions.checkNotNull(providerContext);
        this.extensionActivators = Preconditions.checkNotNull(extensionActivators);
    }

    public void start() {
        LOG.debug("Starting {} BmpExtensionProviderActivator instances", this.extensionActivators.size());

        for(final BmpExtensionProviderActivator e : this.extensionActivators) {
            e.start(this.providerContext);
        }
    }

    @Override
    public void close() {
        LOG.debug("Stopping {} BmpExtensionProviderActivator instances", this.extensionActivators.size());

        for(final BmpExtensionProviderActivator e : this.extensionActivators) {
            e.stop();
        }
    }

}
