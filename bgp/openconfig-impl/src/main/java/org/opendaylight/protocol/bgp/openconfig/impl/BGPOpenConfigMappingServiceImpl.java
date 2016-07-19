/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.add.all.paths.AllPathSelection;
import org.opendaylight.protocol.bgp.mode.impl.add.n.paths.AddPathBestNPathSelection;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigMappingService;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.AfiSafi2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;

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
    public Map<BgpTableType, PathSelectionMode> toPathSelectionMode(final List<AfiSafi> afiSafis) {
        final Map<BgpTableType, PathSelectionMode> pathSelectionModes = new HashMap<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final AfiSafi1 afiSafi1 = afiSafi.getAugmentation(AfiSafi1.class);
            final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName());
            if (afiSafi1 != null && bgpTableType.isPresent()) {
                final Short sendMax = afiSafi1.getSendMax();
                final PathSelectionMode selectionMode;
                if (sendMax > 1) {
                    selectionMode = new AddPathBestNPathSelection(sendMax.longValue());
                } else {
                    selectionMode = new AllPathSelection();
                }
                pathSelectionModes.put(bgpTableType.get(), selectionMode);
            }
        }
        return pathSelectionModes;
    }

    @Override
    public boolean isApplicationPeer(final Neighbor neighbor) {
        return OpenConfigUtil.isAppNeighbor(neighbor);
    }

    @Override
    public PeerRole toPeerRole(final Neighbor neighbor) {
        return OpenConfigUtil.toPeerRole(neighbor);
    }

    @Override
    public List<AddressFamilies> toAddPathCapability(final List<AfiSafi> afiSafis) {
        final List<AddressFamilies> addPathCapability = new ArrayList<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final AfiSafi2 afiSafi2 = afiSafi.getAugmentation(AfiSafi2.class);
            final Optional<BgpTableType> bgpTableType = OpenConfigUtil.toBgpTableType(afiSafi.getAfiSafiName());
            if (afiSafi2 != null && bgpTableType.isPresent()) {
                final AddressFamiliesBuilder builder = new AddressFamiliesBuilder(bgpTableType.get());
                builder.setSendReceive(toSendReceiveMode(afiSafi2));
                addPathCapability.add(builder.build());
            }
        }
        return addPathCapability;
    }

    private static SendReceive toSendReceiveMode(final AfiSafi2 addPath) {
        if (addPath.isReceive() && addPath.getSendMax() != null) {
            return SendReceive.Both;
        }
        if (addPath.getSendMax() != null) {
            return SendReceive.Send;
        }
        return SendReceive.Receive;
    }

}
