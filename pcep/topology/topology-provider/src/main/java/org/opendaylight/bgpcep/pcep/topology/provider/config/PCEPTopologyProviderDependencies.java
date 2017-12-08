/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologySessionListenerFactory;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.protocol.pcep.PCEPDispatcher;

/**
 * Provides required dependencies for PCEPTopologyProviderProvider instantiation
 */
@Beta
public interface PCEPTopologyProviderDependencies {
    /**
     * @return PCEPDispatcher
     */
    @Nonnull
    PCEPDispatcher getPCEPDispatcher();

    /**
     * @return RpcProviderRegistry
     */
    @Nonnull
    RpcProviderRegistry getRpcProviderRegistry();

    /**
     * @return DataBroker
     */
    @Nonnull
    DataBroker getDataBroker();

    /**
     * @return TopologySessionListenerFactory
     */
    @Nonnull
    TopologySessionListenerFactory getTopologySessionListenerFactory();

    /**
     * @return TopologySessionStateRegistry
     */
    @Nonnull
    TopologySessionStatsRegistry getStateRegistry();
}
