/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.yangtools.concepts.Registration;

@Singleton
public final class DefaultBGPTableTypeRegistryProvider extends SimpleBGPTableTypeRegistryProvider
        implements AutoCloseable {
    private final List<Registration> registrations;

    @VisibleForTesting
    public DefaultBGPTableTypeRegistryProvider(final BGPTableTypeRegistryProviderActivator... extensionActivators) {
        this(Arrays.asList(extensionActivators));
    }

    @Inject
    public DefaultBGPTableTypeRegistryProvider(final List<BGPTableTypeRegistryProviderActivator> extensionActivators) {
        registrations = extensionActivators.stream()
            .flatMap(activator -> activator.startBGPTableTypeRegistryProvider(this).stream())
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @PreDestroy
    public void close() {
        registrations.forEach(Registration::close);
    }
}
