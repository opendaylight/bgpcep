/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import io.netty.util.Timer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.protocol.pcep.PCEPDispatcher;

/**
 * Provides required dependencies for PCEPTopologyProviderProvider instantiation.
 */
@NonNullByDefault
interface PCEPTopologyProviderDependencies {
    /**
     * PCEP Dispatcher.
     *
     * @return PCEPDispatcher
     */
    PCEPDispatcher getPCEPDispatcher();

    /**
     * Rpc Provider Registry.
     *
     * @return RpcProviderRegistry
     */
    RpcProviderService getRpcProviderRegistry();

    /**
     * DataBroker.
     *
     * @return DataBroker
     */
    DataBroker getDataBroker();

    /**
     * PCE Server Provider.
     *
     * @return PceServerProvider
     */
    PceServerProvider getPceServerProvider();

    /**
     * Return the timer to use used for scheduling various timeouts.
     *
     * @return A Timer.
     */
    Timer getTimer();

    TopologyStatsScheduler getStatsScheduler();
}
