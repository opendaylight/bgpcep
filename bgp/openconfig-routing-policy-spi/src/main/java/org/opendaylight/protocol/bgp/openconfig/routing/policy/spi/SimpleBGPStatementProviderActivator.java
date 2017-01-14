/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.spi;

import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.registry.StatementRegistryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleBGPStatementProviderActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleBGPStatementProviderActivator.class);

    private final StatementRegistryProvider providerContext;
    private final List<StatementProviderActivator> statementActivators;

    public SimpleBGPStatementProviderActivator(final StatementRegistryProvider providerContext,
        final List<StatementProviderActivator> extensionActivators) {
        this.providerContext = Preconditions.checkNotNull(providerContext);
        this.statementActivators = Preconditions.checkNotNull(extensionActivators);
    }

    public void start() {
        LOG.info("Starting {} StatementProviderActivator instances", this.statementActivators.size());
        this.statementActivators.forEach(activator -> activator.start(this.providerContext));
    }

    @Override
    public void close() {
        LOG.info("Stopping {} StatementProviderActivator instances", this.statementActivators.size());
        this.statementActivators.forEach(StatementProviderActivator::stop);
    }
}
