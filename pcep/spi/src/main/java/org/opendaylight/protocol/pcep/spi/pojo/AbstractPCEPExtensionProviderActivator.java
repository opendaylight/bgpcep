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
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractPCEPExtensionProviderActivator implements AutoCloseable, PCEPExtensionProviderActivator {
    @GuardedBy("this")
    private List<? extends Registration> registrations;

    @Holding("this")
    protected abstract List<? extends Registration> startImpl(PCEPExtensionProviderContext context);

    @Override
    public final synchronized void start(final PCEPExtensionProviderContext context) {
        checkState(this.registrations == null);

        this.registrations = requireNonNull(startImpl(context));
    }

    @Override
    public final synchronized void stop() {
        if (this.registrations == null) {
            return;
        }

        this.registrations.forEach(Registration::close);
        this.registrations = null;
    }

    @Override
    public final void close() {
        stop();
    }
}
