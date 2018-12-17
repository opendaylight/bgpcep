/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPCEPExtensionProviderActivator implements AutoCloseable, PCEPExtensionProviderActivator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCEPExtensionProviderActivator.class);

    @GuardedBy("this")
    private List<AutoCloseable> registrations;

    @GuardedBy("this")
    protected abstract List<AutoCloseable> startImpl(PCEPExtensionProviderContext context);

    @Override
    public final synchronized void start(final PCEPExtensionProviderContext context) {
        checkState(this.registrations == null);

        this.registrations = requireNonNull(startImpl(context));
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
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
