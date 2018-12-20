/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractBGPExtensionProviderActivator implements AutoCloseable, BGPExtensionProviderActivator {
    @GuardedBy("this")
    private List<? extends Registration> registrations;

    @GuardedBy("this")
    protected abstract List<? extends Registration> startImpl(BGPExtensionProviderContext context);

    @Override
    public final synchronized void start(final BGPExtensionProviderContext context) {
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
