package org.opendaylight.protocol.bmp.spi.registry;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;

import java.util.List;

import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 15.5.2015.
 */
public abstract class AbstractBMPExtensionProviderActivator implements AutoCloseable, BmpExtensionProviderActivator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBMPExtensionProviderActivator.class);

    @GuardedBy("this")
    private List<AutoCloseable> registrations;

    @GuardedBy("this")
    protected abstract List<AutoCloseable> startImpl(BmpExtensionProviderContext context);

    @Override
    public final void close() {
        stop();
    }

    @Override
    public final synchronized void start(final BmpExtensionProviderContext context) {
        Preconditions.checkState(this.registrations == null);
        this.registrations = Preconditions.checkNotNull(startImpl(context));
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
