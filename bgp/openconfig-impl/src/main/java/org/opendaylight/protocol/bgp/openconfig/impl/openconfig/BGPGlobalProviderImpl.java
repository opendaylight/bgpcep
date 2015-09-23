/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.BGP_IID;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BGPGlobalProviderImpl extends AbstractBGPOpenConfigMapper<BGPRibInstanceConfiguration, Global> {

    private static final InstanceIdentifier<Global> GLOBAL_IID = BGP_IID.child(Global.class);

    public BGPGlobalProviderImpl(final BindingTransactionChain txChain, final BGPConfigStateStore stateHolders) {
        super(txChain, stateHolders, Global.class);
    }

    @Override
    public Global apply(final BGPRibInstanceConfiguration configuration) {
        return toGlobalConfiguration(configuration);
    }

    @Override
    public ModuleKey createModuleKey(final String instanceName) {
        return new ModuleKey(instanceName, RibImpl.class);
    }

    @Override
    protected InstanceIdentifier<Global> getInstanceIdentifier(final Identifier key) {
        return GLOBAL_IID;
    }

    @Override
    public Identifier keyForConfiguration(final Global global) {
        return GlobalIdentifier.GLOBAL_IDENTIFIER;
    }

    @Override
    public Class<BGPRibInstanceConfiguration> getInstanceConfigurationType() {
        return BGPRibInstanceConfiguration.class;
    }

    private static Global toGlobalConfiguration(final BGPRibInstanceConfiguration config) {
        return new GlobalBuilder()
        .setAfiSafis(new AfiSafisBuilder().setAfiSafi(OpenConfigUtil.toAfiSafis(config.getTableTypes())).build())
        .setConfig(
            new ConfigBuilder()
                .setAs(config.getLocalAs())
                .setRouterId(config.getBgpRibId()).build()).build();
    }

}
