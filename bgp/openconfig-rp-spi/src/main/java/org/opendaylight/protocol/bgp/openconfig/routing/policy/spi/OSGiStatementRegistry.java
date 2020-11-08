/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { StatementRegistryConsumer.class, StatementRegistryProvider.class })
public final class OSGiStatementRegistry extends ForwardingStatementRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiStatementRegistry.class);

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    List<StatementProviderActivator> activators;

    private StatementRegistry delegate;

    @Override
    protected StatementRegistry delegate() {
        return verifyNotNull(delegate);
    }

    @Activate
    void activate() {
        delegate = new StatementRegistry();
        LOG.info("Starting {} StatementProviderActivator instances", activators.size());
        activators.forEach(activator -> activator.start(delegate));
    }

    @Deactivate
    void deactivate() {
        LOG.info("Stopping {} StatementProviderActivator instances", activators.size());
        activators.forEach(StatementProviderActivator::stop);
        delegate = null;
    }
}
