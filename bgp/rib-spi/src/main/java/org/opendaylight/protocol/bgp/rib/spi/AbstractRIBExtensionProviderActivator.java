/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRIBExtensionProviderActivator implements AutoCloseable, RIBExtensionProviderActivator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRIBExtensionProviderActivator.class);

    @GuardedBy("this")
    private List<AutoCloseable> registrations;

    @GuardedBy("this")
    protected abstract List<AutoCloseable> startRIBExtensionProviderImpl(RIBExtensionProviderContext context);

    @Override
    public final synchronized void startRIBExtensionProvider(final RIBExtensionProviderContext context) {
        Preconditions.checkState(this.registrations == null);

        this.registrations = requireNonNull(startRIBExtensionProviderImpl(context));
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public final synchronized void stopRIBExtensionProvider() {
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
        stopRIBExtensionProvider();
    }
}
