/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;
import java.net.InetAddress;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.pcep.topology.provider.config.rev170301.odl.pcep.topology.provider.odl.pcep.topology.provider.config.Client;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * The PCEPTopologyDeployer service is managing PcepTopologyProvider
 */
public interface PCEPTopologyDeployer {
    /**
     * Register PCEP RootRuntimeBean
     *
     * @param topologyID                        topology ID
     * @param rootRuntimeBeanRegistratorWrapper PCEPTopologyProviderRuntimeRegistrator
     */
    void addRootRuntimeBeanRegistratorWrapper(@Nonnull TopologyId topologyID,
        @Nullable PCEPTopologyProviderRuntimeRegistrator rootRuntimeBeanRegistratorWrapper);

    /**
     * Unregister PCEP RootRuntimeBean
     *
     * @param topologyID topology ID
     */
    void removeRootRuntimeBeanRegistratorWrapper(@Nonnull TopologyId topologyID);

    /**
     * Writes Topology Provider Config on DS
     *
     * @param topologyID    topology Id
     * @param instructionID
     * @param address
     * @param portNumber
     * @param rpcTimeout
     * @param client
     * @return
     */
    ListenableFuture<Void> writeConfiguration(@Nonnull TopologyId topologyID, @Nonnull String instructionID,
        @Nonnull InetAddress address, @Nonnull PortNumber portNumber, short rpcTimeout, @Nullable List<Client> client);

    /**
     * Remove instruction configuration on DS
     *
     * @param topologyID topology Id
     * @return
     */
    ListenableFuture<Void> removeConfiguration(@Nonnull TopologyId topologyID);
}
