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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;

final class GlobalComparator implements OpenConfigComparator<Bgp> {

    @Override
    public boolean isSame(final Bgp bgp1, final Bgp bgp2) {
        Preconditions.checkNotNull(bgp1);
        Preconditions.checkNotNull(bgp2);
        final Global global1 = bgp1.getGlobal();
        final Global global2 = bgp2.getGlobal();
        //do not care about an order of collections' entries
        if (global1.getAfiSafis() != null && global2.getAfiSafis() != null) {
            final List<AfiSafi> afiSafiA = global1.getAfiSafis().getAfiSafi();
            final List<AfiSafi> afiSafiB = global2.getAfiSafis().getAfiSafi();
            if (afiSafiA.size() != afiSafiB.size()) {
                return false;
            }
            if (!afiSafiA.containsAll(afiSafiB) && !afiSafiB.containsAll(afiSafiA)) {
                return false;
            }
        } else if (global1.getAfiSafis() != null || global2.getAfiSafis() != null) {
            return false;
        }
        if (!Objects.equals(global1.getConfig(), global2.getConfig())) {
            return false;
        }
        return true;
    }

}
