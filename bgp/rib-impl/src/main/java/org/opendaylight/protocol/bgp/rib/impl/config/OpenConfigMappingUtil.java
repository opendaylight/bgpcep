/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil.INSTANCE;

import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class OpenConfigMappingUtil {

    private OpenConfigMappingUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getRibInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return rootIdentifier.firstKeyOf(Protocol.class).getName();
    }

    public static int getHoldTimer(final Neighbor neighbor) {
        return neighbor.getTimers().getConfig().getHoldTime().intValue();
    }

    public static AsNumber getPeerAs(final Neighbor neighbor, final RIB rib) {
        final AsNumber peerAs = neighbor.getConfig().getPeerAs();
        if (peerAs != null) {
            return peerAs;
        }
        return rib.getLocalAs();
    }

    public static boolean isActive(final Neighbor neighbor) {
        return !neighbor.getTransport().getConfig().isPassiveMode();
    }

    public static int getRetryTimer(final Neighbor neighbor) {
        return neighbor.getTimers().getConfig().getConnectRetry().intValue();
    }

    public static KeyMapping getNeighborKey(final Neighbor neighbor) {
        final String authPassword = neighbor.getConfig().getAuthPassword();
        if (authPassword != null) {
            KeyMapping.getKeyMapping(INSTANCE.inetAddressFor(neighbor.getNeighborAddress()), authPassword);
        }
        return null;
    }

    public static InstanceIdentifier<Neighbor> getNeighborInstanceIdentifier(final InstanceIdentifier<Bgp> rootIdentifier,
            final NeighborKey neighborKey) {
        return rootIdentifier.child(Neighbors.class).child(Neighbor.class, neighborKey);
    }

}
