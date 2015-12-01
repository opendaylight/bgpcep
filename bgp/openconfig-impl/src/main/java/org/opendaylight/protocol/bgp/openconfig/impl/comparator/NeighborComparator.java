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
        if (!Objects.equals(neighbor1.getConfig(), neighbor2.getConfig())) {
            return false;
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
        if (!Objects.equals(neighbor1.getTimers(), neighbor2.getTimers())) {
            return false;
        }
        if (!Objects.equals(neighbor1.getTransport(), neighbor2.getTransport())) {
            return false;
        }
        if (!Objects.equals(neighbor1.getAugmentation(Neighbor1.class), neighbor2.getAugmentation(Neighbor1.class))) {
            return false;
        }
        return true;
    }

}
