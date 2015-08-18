/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRSVPExtensionProviderActivator implements AutoCloseable, RSVPExtensionProviderActivator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRSVPExtensionProviderActivator.class);

    @GuardedBy("this")
    private List<AutoCloseable> registrations;

    @GuardedBy("this")
    protected abstract List<AutoCloseable> startImpl(RSVPExtensionProviderContext context);

    @Override
    public final synchronized void start(final RSVPExtensionProviderContext context) {
        Preconditions.checkState(this.registrations == null);

        this.registrations = Preconditions.checkNotNull(startImpl(context));
    }

    @Override
    public final synchronized void stop() {
        if (this.registrations == null) {
            return;
        }

        for (final AutoCloseable r : this.registrations) {
            try {
                r.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close registration", e);
            }
        }

        this.registrations = null;
    }

    @Override
    public final void close() {
        stop();
    }
}