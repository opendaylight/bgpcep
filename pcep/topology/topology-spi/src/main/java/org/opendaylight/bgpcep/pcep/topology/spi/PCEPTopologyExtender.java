/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.spi;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * A extender attaching to PCEP-capable topologies. Takze care of lifecycle on
 *
 */
public interface PCEPTopologyExtender {


    ListenableFuture<PCEPTopologyExtension> startExtension(KeyedInstanceIdentifier<Topology, TopologyKey> topologyPath,
        InstructionScheduler instructionScheduler);
}
