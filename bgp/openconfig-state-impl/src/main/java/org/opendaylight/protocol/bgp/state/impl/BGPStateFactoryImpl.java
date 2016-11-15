/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl;

import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.state.spi.BGPStateFactory;
import org.opendaylight.protocol.bgp.state.spi.state.BGPGlobalState;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class BGPStateFactoryImpl implements BGPStateFactory {
    @Override
    public BGPGlobalState createBGPGlobalState(final Set<Class<? extends AfiSafiType>> announcedAfiSafis,
        final String ribId, final BgpId routeId, final AsNumber localAs,
        final InstanceIdentifier<Bgp> bgpIId) {
        return new BGPGlobalStateImpl(announcedAfiSafis, ribId, routeId, localAs, bgpIId);
    }

    @Override
    public BGPNeighborState createBGPNeighborState(@Nonnull final IpAddress neighborAddress,
        final Set<Class<? extends AfiSafiType>> afiSafisAdvertized,
        final Set<Class<? extends AfiSafiType>> afiSafisGracefulAdvertized) {
        return new BGPNeighborStateImpl(neighborAddress, afiSafisAdvertized, afiSafisGracefulAdvertized);
    }
}
