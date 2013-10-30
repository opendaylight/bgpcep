/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.topology.pcep.type.TopologyPcep;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class PCEPTunnelInstructionExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(PCEPTunnelInstructionExecutor.class);
	private final InstanceIdentifier<Topology> underlay;
	private final InstanceIdentifier<Topology> target;
	private final DataProviderService dps;

	private PCEPTunnelInstructionExecutor(final DataProviderService dps, final InstanceIdentifier<Topology> underlay,
			final InstanceIdentifier<Topology> target, final TopologyId topologyId) {
		this.dps = Preconditions.checkNotNull(dps);
		this.underlay = Preconditions.checkNotNull(underlay);
		this.target = Preconditions.checkNotNull(target);
	}

	public static PCEPTunnelInstructionExecutor create(final DataProviderService dps, final InstanceIdentifier<Topology> underlay,
			final TopologyId targetId) {
		Preconditions.checkNotNull(dps);
		Preconditions.checkNotNull(targetId);

		// Topology pointer
		final InstanceIdentifier<Topology> target = InstanceIdentifier.builder().node(NetworkTopology.class).
				node(Topology.class, new TopologyKey(targetId)).toInstance();

		// Now check if there is a container identifying the topology as PCEP-aware
		final InstanceIdentifier<TopologyPcep> i = InstanceIdentifier.builder(target).node(TopologyTypes.class).node(TopologyPcep.class).toInstance();
		final DataObject ttt = dps.readOperationalData(i);
		Preconditions.checkArgument(ttt != null, "Specified topology does not list topology-pcep as its type");

		return new PCEPTunnelInstructionExecutor(dps, underlay, target, targetId);
	}
}
