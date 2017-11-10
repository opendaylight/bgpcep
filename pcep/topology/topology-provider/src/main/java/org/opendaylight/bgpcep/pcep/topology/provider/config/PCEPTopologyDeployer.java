/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

/**
 * The PCEPTopologyDeployer service is managing PcepTopologyProvider
 */
public interface PCEPTopologyDeployer {
    /**
     * Writes Topology Provider Config on DS
     *
     * @param pcepTopology pcepTopology
     * @return future
     */
    ListenableFuture<Void> writeConfiguration(@Nonnull Topology pcepTopology);

    /**
     * Remove instruction configuration on DS
     *
     * @param topologyId topology Id
     * @return future
     */
    ListenableFuture<Void> removeConfiguration(@Nonnull TopologyId topologyId);
}
