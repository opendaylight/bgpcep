/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Starts and stops PCEPExtensionProviderActivator instances for a PCEPExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
public class SimplePCEPExtensionProviderContextActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SimplePCEPExtensionProviderContextActivator.class);

    private final PCEPExtensionProviderContext providerContext;
    private final List<PCEPExtensionProviderActivator> extensionActivators;

    public SimplePCEPExtensionProviderContextActivator(final PCEPExtensionProviderContext providerContext,
            final List<PCEPExtensionProviderActivator> extensionActivators) {
        this.providerContext = Preconditions.checkNotNull(providerContext);
        this.extensionActivators = Preconditions.checkNotNull(extensionActivators);
    }

    public void start() {
        LOG.debug("Starting {} PCEPExtensionProviderActivator instances", extensionActivators.size());

        for(final PCEPExtensionProviderActivator e : extensionActivators) {
            e.start(providerContext);
        }
    }

    @Override
    public void close() {
        LOG.debug("Stopping {} BGPExtensionProviderActivator instances", extensionActivators.size());

        for(final PCEPExtensionProviderActivator e : extensionActivators) {
            e.stop();
        }
    }
}
