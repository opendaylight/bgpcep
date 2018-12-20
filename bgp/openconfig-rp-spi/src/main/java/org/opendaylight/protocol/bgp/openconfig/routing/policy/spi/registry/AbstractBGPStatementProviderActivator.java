/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractBGPStatementProviderActivator implements StatementProviderActivator, AutoCloseable {
    @GuardedBy("this")
    private List<? extends Registration> registrations;

    @GuardedBy("this")
    protected abstract List<? extends Registration> startImpl(StatementRegistryProvider context);

    @Override
    public final synchronized void start(final StatementRegistryProvider context) {
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
    public final synchronized void close() {
        stop();
    }
}
