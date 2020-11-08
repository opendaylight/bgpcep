/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistry;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryConsumer;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class SimpleStatementRegistry extends ForwardingStatementRegistry
        implements AutoCloseable, StatementRegistryConsumer, StatementRegistryProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleStatementRegistry.class);

    private final @NonNull StatementRegistry registry = new StatementRegistry();
    private final List<StatementProviderActivator> activators;

    @Inject
    public SimpleStatementRegistry(final List<StatementProviderActivator> extensionActivators) {
        this.activators = ImmutableList.copyOf(extensionActivators);
    }

    @Override
    protected StatementRegistry delegate() {
        return registry;
    }

    @PostConstruct
    public void start() {
        LOG.info("Starting {} StatementProviderActivator instances", activators.size());
        activators.forEach(activator -> activator.start(registry));
    }

    @PreDestroy
    @Override
    public void close() {
        LOG.info("Stopping {} StatementProviderActivator instances", activators.size());
        activators.forEach(StatementProviderActivator::stop);
    }
}
