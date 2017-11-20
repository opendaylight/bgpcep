/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * The BgpDeployer service is managing RIB, Peer, Application Peer instances based on the OpenConfig BGP
 * configuration status.
 * BGP configuration is held under the specific OpenConfig's NetworkInstance subtree.
 */
@Deprecated
public interface BgpDeployer {

    interface WriteConfiguration {
        void apply();
    }

    /**
     * Get pointer to NetworkInstance instance where this particular BGP deployer is binded.
     *
     * @return InstanceIdentifier
     */
    InstanceIdentifier<NetworkInstance> getInstanceIdentifier();

    BGPTableTypeRegistryConsumer getTableTypeRegistry();

    <T extends DataObject> ListenableFuture<Void> writeConfiguration(T data, InstanceIdentifier<T> identifier);

    <T extends DataObject> ListenableFuture<Void> removeConfiguration(InstanceIdentifier<T> identifier);

    /**
     * Create, start and register rib instance
     * @param rootIdentifier
     * @param global
     * @param configurationWriter
     */
    void onGlobalModified(InstanceIdentifier<Bgp> rootIdentifier, Global global, WriteConfiguration configurationWriter);

    /**
     * Destroy rib instance
     * @param rootIdentifier
     */
    void onGlobalRemoved(InstanceIdentifier<Bgp> rootIdentifier);

    /**
     * Create, start and register peer instance
     * @param rootIdentifier
     * @param neighbor
     */
    void onNeighborModified(InstanceIdentifier<Bgp> rootIdentifier, Neighbor neighbor, WriteConfiguration configurationWriter);

    /**
     * Destroy peer instance
     * @param rootIdentifier
     * @param neighbor
     */
    void onNeighborRemoved(InstanceIdentifier<Bgp> rootIdentifier, Neighbor neighbor);
}
