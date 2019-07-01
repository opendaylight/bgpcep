/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.List;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractRSVPExtensionProviderActivator implements AutoCloseable, RSVPExtensionProviderActivator {
    @GuardedBy("this")
    private List<? extends Registration> registrations;

    @Holding("this")
    protected abstract List<? extends Registration> startImpl(RSVPExtensionProviderContext context);

    @Override
    public final synchronized void start(final RSVPExtensionProviderContext context) {
        checkState(this.registrations == null);

        this.registrations = requireNonNull(startImpl(context));
    }

    @Override
    public final synchronized void stop() {
        if (this.registrations != null) {
            this.registrations.forEach(Registration::close);
            this.registrations = null;
        }
    }

    @Override
    public final void close() {
        stop();
    }
}
