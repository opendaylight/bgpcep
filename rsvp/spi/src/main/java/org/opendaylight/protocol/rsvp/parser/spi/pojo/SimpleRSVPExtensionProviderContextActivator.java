/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops RSVPExtensionProviderActivator instances for a RSVPExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
public class SimpleRSVPExtensionProviderContextActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRSVPExtensionProviderContextActivator.class);

    private final RSVPExtensionProviderContext providerContext;
    private final List<RSVPExtensionProviderActivator> extensionActivators;

    public SimpleRSVPExtensionProviderContextActivator(final RSVPExtensionProviderContext providerContext,
            final List<RSVPExtensionProviderActivator> extensionActivators) {
        this.providerContext = Preconditions.checkNotNull(providerContext);
        this.extensionActivators = Preconditions.checkNotNull(extensionActivators);
    }

    public void start() {
        LOG.debug("Starting {} RSVPExtensionProviderActivator instances", this.extensionActivators.size());

        for(final RSVPExtensionProviderActivator e : this.extensionActivators) {
            e.start(this.providerContext);
        }
    }

    @Override
    public void close() {
        LOG.debug("Stopping {} RSVPExtensionProviderActivator instances", this.extensionActivators.size());

        for(final RSVPExtensionProviderActivator e : this.extensionActivators) {
            e.stop();
        }
    }
}
