/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractRIBExtensionProviderActivator implements AutoCloseable, RIBExtensionProviderActivator {
    @GuardedBy("this")
    private List<? extends Registration> registrations;

    @GuardedBy("this")
    protected abstract List<? extends Registration> startRIBExtensionProviderImpl(
            RIBExtensionProviderContext context,
            BindingNormalizedNodeSerializer mappingService);

    @Override
    public final synchronized void startRIBExtensionProvider(
            final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        checkState(this.registrations == null);

        this.registrations = requireNonNull(startRIBExtensionProviderImpl(context, mappingService));
    }

    @Override
    public final synchronized void stopRIBExtensionProvider() {
        if (this.registrations == null) {
            return;
        }

        this.registrations.forEach(Registration::close);
        this.registrations = null;
    }

    @Override
    public final void close() {
        stopRIBExtensionProvider();
    }
}
