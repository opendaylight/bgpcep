/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
import org.osgi.framework.BundleContext;

final class TunnelProviderDependencies {
    private final DataBroker dataBroker;
    private final ClusterSingletonServiceProvider cssp;
    private final NetworkTopologyPcepService ntps;
    private final RpcProviderService rpcProviderRegistry;
    private final BundleContext bundleContext;

    TunnelProviderDependencies(
            final DataBroker dataBroker,
            final ClusterSingletonServiceProvider cssp,
            final RpcProviderService rpcProviderRegistry,
            final RpcConsumerRegistry rpcConsumerRegistry,
            final BundleContext bundleContext
    ) {

        this.dataBroker = requireNonNull(dataBroker);
        this.cssp = requireNonNull(cssp);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.bundleContext = requireNonNull(bundleContext);
        this.ntps = rpcConsumerRegistry.getRpcService(NetworkTopologyPcepService.class);
    }

    DataBroker getDataBroker() {
        return this.dataBroker;
    }

    ClusterSingletonServiceProvider getCssp() {
        return this.cssp;
    }

    NetworkTopologyPcepService getNtps() {
        return this.ntps;
    }

    RpcProviderService getRpcProviderRegistry() {
        return this.rpcProviderRegistry;
    }

    BundleContext getBundleContext() {
        return this.bundleContext;
    }
}
