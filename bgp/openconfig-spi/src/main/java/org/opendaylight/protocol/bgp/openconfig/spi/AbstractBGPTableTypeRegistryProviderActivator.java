/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public abstract class AbstractBGPTableTypeRegistryProviderActivator
        implements AutoCloseable, BGPTableTypeRegistryProviderActivator {

    @GuardedBy("this")
    private List<AbstractRegistration> registrations;

    @GuardedBy("this")
    protected abstract List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
            BGPTableTypeRegistryProvider provider);

    @Override
    public final synchronized void startBGPTableTypeRegistryProvider(final BGPTableTypeRegistryProvider provider) {
        Preconditions.checkState(this.registrations == null);
        this.registrations = requireNonNull(startBGPTableTypeRegistryProviderImpl(provider));
    }

    @Override
    public final synchronized void stopBGPTableTypeRegistryProvider() {
        if (this.registrations == null) {
            return;
        }
        this.registrations.forEach(AbstractRegistration::close);
        this.registrations = null;
    }

    @Override
    public final void close() {
        stopBGPTableTypeRegistryProvider();
    }
}
