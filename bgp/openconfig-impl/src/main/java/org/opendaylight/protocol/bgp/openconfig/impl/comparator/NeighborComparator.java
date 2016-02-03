/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.comparator;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.OpenConfigComparator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Neighbor1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;

final class NeighborComparator implements OpenConfigComparator<Neighbor> {

    @Override
    public boolean isSame(final Neighbor neighbor1, final Neighbor neighbor2) {
        Preconditions.checkNotNull(neighbor1);
        Preconditions.checkNotNull(neighbor2);
        //do not care about an order of collections' entries
        if (neighbor1.getAfiSafis() != null && neighbor2.getAfiSafis() != null) {
            final List<AfiSafi> afiSafiA = neighbor1.getAfiSafis().getAfiSafi();
            final List<AfiSafi> afiSafiB = neighbor2.getAfiSafis().getAfiSafi();
            if (afiSafiA.size() != afiSafiB.size()) {
                return false;
            }
            if (!afiSafiA.containsAll(afiSafiB) && !afiSafiB.containsAll(afiSafiA)) {
                return false;
            }
        } else if (neighbor1.getAfiSafis() != null || neighbor2.getAfiSafis() != null) {
            return false;
        }
        final Config config1 = neighbor1.getConfig();
        final Config config2 = neighbor2.getConfig();
        if (config1 != null && config2 != null) {
            if (!Objects.equals(config1.getPeerAs(), config2.getPeerAs())) {
                return false;
            }
            if (!Objects.equals(config1.getPeerType(), config2.getPeerType())) {
                return false;
            }
        }
        if (!Objects.equals(neighbor1.getKey(), neighbor2.getKey())) {
            return false;
        }
        if (!Objects.equals(neighbor1.getNeighborAddress(), neighbor2.getNeighborAddress())) {
            return false;
        }
        if (!Objects.equals(neighbor1.getRouteReflector(), neighbor2.getRouteReflector())) {
            return false;
        }
        final Timers timers1 = neighbor1.getTimers();
        final Timers timers2 = neighbor2.getTimers();
        if (timers1 != null && timers2 != null) {
            if (!Objects.equals(timers1.getConfig().getHoldTime(), timers2.getConfig().getHoldTime())) {
                return false;
            }
        }
        final Transport transport1 = neighbor1.getTransport();
        final Transport transport2 = neighbor2.getTransport();
        if (transport1 != null && transport2 != null) {
            if (!Objects.equals(transport1.getConfig().isPassiveMode(), transport2.getConfig().isPassiveMode())) {
                return false;
            }
        }
        if (!Objects.equals(neighbor1.getAugmentation(Neighbor1.class), neighbor2.getAugmentation(Neighbor1.class))) {
            return false;
        }
        return true;
    }

}
