/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.tunnel;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.BaseTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class TunnelProgrammingUtil {
    private TunnelProgrammingUtil() {
        throw new UnsupportedOperationException();
    }

    public static InstanceIdentifier<Link> linkIdentifier(final InstanceIdentifier<Topology> topology,
            final BaseTunnelInput input) {
        requireNonNull(input.getLinkId());
        return topology.child(Link.class, new LinkKey(input.getLinkId()));
    }
}
