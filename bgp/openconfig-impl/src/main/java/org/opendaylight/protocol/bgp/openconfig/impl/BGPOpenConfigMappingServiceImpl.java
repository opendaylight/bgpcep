/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;

public final class BGPOpenConfigMappingServiceImpl implements BGPOpenConfigMappingService {

    @Override
    public List<BgpTableType> toTableTypes(final List<AfiSafi> afiSafis) {
        return afiSafis.stream()
                .map(afiSafi -> OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AfiSafi> toAfiSafi(final BgpTableType tableType) {
        return OpenConfigUtil.toAfiSafi(tableType);
    }

    @Override
    public Optional<BgpTableType> toBgpTableType(final Class<? extends AfiSafiType> afiSafi) {
        return OpenConfigUtil.toBgpTableType(afiSafi);
    }

}
