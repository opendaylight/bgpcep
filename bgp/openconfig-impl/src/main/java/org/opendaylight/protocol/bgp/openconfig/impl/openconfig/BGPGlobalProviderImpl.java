/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.APPLICATION_PEER_GROUP_NAME;
import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.BGP_IID;

import java.util.Collections;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.BgpBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.NeighborsBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.PeerGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BGPGlobalProviderImpl extends AbstractBGPOpenConfigMapper<BGPRibInstanceConfiguration, Bgp> {

    private static final PeerGroup APP_PEER_GROUP = new PeerGroupBuilder().setPeerGroupName(APPLICATION_PEER_GROUP_NAME)
            .setKey(new PeerGroupKey(APPLICATION_PEER_GROUP_NAME)).build();

    public BGPGlobalProviderImpl(final BindingTransactionChain txChain, final BGPConfigStateStore stateHolders) {
        super(txChain, stateHolders, Bgp.class);
    }

    @Override
    public Bgp apply(final BGPRibInstanceConfiguration configuration) {
        return toGlobalConfiguration(configuration);
    }

    @Override
    public ModuleKey createModuleKey(final String instanceName) {
        return new ModuleKey(instanceName, RibImpl.class);
    }

    @Override
    protected InstanceIdentifier<Bgp> getInstanceIdentifier(final Identifier key) {
        return BGP_IID;
    }

    @Override
    public Identifier keyForConfiguration(final Bgp bgp) {
        return GlobalIdentifier.GLOBAL_IDENTIFIER;
    }

    @Override
    public Class<BGPRibInstanceConfiguration> getInstanceConfigurationType() {
        return BGPRibInstanceConfiguration.class;
    }

    private static Bgp toGlobalConfiguration(final BGPRibInstanceConfiguration config) {
        final BgpBuilder bgpBuilder = new BgpBuilder();
        bgpBuilder.setNeighbors(new NeighborsBuilder().build());
        bgpBuilder.setPeerGroups(new PeerGroupsBuilder().setPeerGroup(Collections.singletonList(APP_PEER_GROUP)).build());
        final Global global = new GlobalBuilder()
            .setAfiSafis(new AfiSafisBuilder().setAfiSafi(OpenConfigUtil.toAfiSafis(config.getTableTypes())).build())
            .setConfig(new ConfigBuilder()
                .setAs(config.getLocalAs())
                .setRouterId(config.getBgpRibId()).build()).build();
        bgpBuilder.setGlobal(global);
        return bgpBuilder.build();
    }

}
