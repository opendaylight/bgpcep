/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.tunnel;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.BaseTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class TunnelProgrammingUtil {
	private TunnelProgrammingUtil() {

	}

	public static InstanceIdentifier<Link> linkIdentifier(final InstanceIdentifier<Topology> topology, final BaseTunnelInput input) {
		Preconditions.checkNotNull(input.getLinkId());
		return InstanceIdentifier.builder(topology).child(Link.class, new LinkKey(input.getLinkId())).toInstance();
	}
}
