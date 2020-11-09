/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = BGPTableTypeRegistryConsumer.class)
public final class OSGiBGPTableTypeRegistryProvider extends SimpleBGPTableTypeRegistryProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiBGPTableTypeRegistryProvider.class);

    @Reference
    List<BGPTableTypeRegistryProviderActivator> extensionActivators;

    private List<Registration> registrations;

    @Activate
    void activate() {
        LOG.info("BGPTableTypeRegistryProviderActivator starting with {} extensions", extensionActivators.size());
        registrations = extensionActivators.stream()
            .flatMap(activator -> activator.startBGPTableTypeRegistryProvider(this).stream())
            .collect(Collectors.toUnmodifiableList());
        LOG.info("BGPTableTypeRegistryProvider started with {} registrations", registrations.size());
    }

    @Deactivate
    void deactivate() {
        registrations.forEach(Registration::close);
        registrations = null;
        LOG.info("BGPTableTypeRegistryProvider stopped");
    }
}
