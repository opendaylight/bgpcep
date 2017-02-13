/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import java.net.InetAddress;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.config.yang.pcep.topology.provider.Client;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

/**
 * The PCEPTopologyDeployer service is managing PcepTopologyProvider
 */
public interface PCEPTopologyDeployer {
    /**
     * Creates and register topology provider instance
     *
     * @param topologyId topology ID
     * @param address    ipAddress
     * @param port       port
     * @param rpcTimeout rpc Timeout
     * @param client     List of clients password configuration
     * @param scheduler  Instruction Scheduler
     */
    void createTopologyProvider(@Nonnull TopologyId topologyId, @Nonnull final InetAddress address, final int port,
        final short rpcTimeout, @Nullable final List<Client> client,
        @Nonnull final InstructionScheduler scheduler);

    /**
     * Closes and unregister topology provider instance
     *
     * @param topologyID topology ID
     */
    void removeTopologyProvider(@Nonnull final TopologyId topologyID);
}
