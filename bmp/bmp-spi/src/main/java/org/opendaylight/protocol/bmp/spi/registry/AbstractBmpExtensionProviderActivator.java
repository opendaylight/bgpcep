/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.registry;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 15.5.2015.
 */
public abstract class AbstractBmpExtensionProviderActivator implements AutoCloseable, BmpExtensionProviderActivator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBmpExtensionProviderActivator.class);

    @GuardedBy("this")
    private List<AutoCloseable> registrations;

    @GuardedBy("this")
    protected abstract List<AutoCloseable> startImpl(final BmpExtensionProviderContext context);

    @Override
    public final void close() {
        stop();
    }

    @Override
    public final synchronized void start(@Nonnull final BmpExtensionProviderContext context) {
        Preconditions.checkState(this.registrations == null);
        this.registrations = requireNonNull(startImpl(context));
    }

    @Override
    public final synchronized void stop() {
        Preconditions.checkState(this.registrations != null);

        for (final AutoCloseable r : this.registrations) {
            try {
                r.close();
            } catch (final Exception e) {
                LOG.warn("Failed to close registration", e);
            }
        }

        this.registrations = null;
    }
}
