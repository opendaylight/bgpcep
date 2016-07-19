/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;

/**
 * The BgpDeployer service is managing RIB, Peer, Application Peer instances based on the OpenConfig BGP
 * configuration status.
 * BGP configuration is held under the specific OpenConfig's NetworkInstance subtree.
 *
 */
public interface BgpDeployer {

    /**
     * Get pointer to NetworkInstance instance where this particular BGP deployer is binded.
     * @return InstanceIdentifier
     */
    InstanceIdentifier<NetworkInstance> getInstanceIdentifier();

    <T extends DataObject> ListenerRegistration<?> registerDataTreeChangeListener(DataTreeChangeListener<T> listener, InstanceIdentifier<T> path);

    BGPOpenConfigMappingService getMappingService();

    Object getComponentInstance(InstanceType instanceType);

    <T> ServiceRegistration<T> registerService(InstanceType instanceType, T service, String instanceName);

}
