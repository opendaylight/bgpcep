/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPAppPeerInstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.Config1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.BgpApplicationPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;

final class BGPAppNeighborProviderImpl extends AbstractBGPNeighborProvider<BGPAppPeerInstanceConfiguration> {

    public BGPAppNeighborProviderImpl(final BindingTransactionChain txChain, final BGPConfigStateStore stateHolders) {
        super(txChain, stateHolders, Neighbor.class);
    }

    @Override
    public ModuleKey createModuleKey(final String instanceName) {
        return new ModuleKey(instanceName, BgpApplicationPeer.class);
    }

    @Override
    public Neighbor apply(final BGPAppPeerInstanceConfiguration config) {
        return toAppNeighbor(config);
    }

    @Override
    public Class<BGPAppPeerInstanceConfiguration> getInstanceConfigurationType() {
        return BGPAppPeerInstanceConfiguration.class;
    }

    private static Neighbor toAppNeighbor(final BGPAppPeerInstanceConfiguration config) {
        final IpAddress ipAddress = new IpAddress(new Ipv4Address(config.getBgpId().getValue()));
        return new NeighborBuilder()
            .setNeighborAddress(ipAddress)
            .setKey(new NeighborKey(ipAddress))
            .setConfig(
                    new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder()
                    .addAugmentation(Config1.class, new Config1Builder().setPeerGroup(OpenConfigUtil.APPLICATION_PEER_GROUP_NAME).build())
                    .build())
            .build();
    }
}
