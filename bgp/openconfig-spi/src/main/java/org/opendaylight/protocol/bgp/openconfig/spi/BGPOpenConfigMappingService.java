/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import java.util.List;
import java.util.Map;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;

public interface BGPOpenConfigMappingService {

    List<BgpTableType> toTableTypes(List<AfiSafi> afiSafis);

    Map<BgpTableType, PathSelectionMode> toPathSelectionMode(List<AfiSafi> afiSafis);

    List<AddressFamilies> toAddPathCapability(List<AfiSafi> afiSafis);

    boolean isApplicationPeer(Neighbor neighbor);

    PeerRole toPeerRole(Neighbor neighbor);
}
