/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.impl.util.GlobalIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.pojo.BGPRibInstanceConfiguration;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.global.base.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.GlobalBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev150930.Linkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BGPGlobalProviderImplTest {

    private BGPGlobalProviderImpl globalProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        final BindingTransactionChain txChain = Mockito.mock(BindingTransactionChain.class);
        final BGPConfigStateStore stateHolders = Mockito.mock(BGPConfigStateStore.class);
        final BGPConfigHolder<Global> configHolder = Mockito.mock(BGPConfigHolder.class);
        Mockito.doReturn(configHolder).when(stateHolders).getBGPConfigHolder(Mockito.any(Class.class));
        this.globalProvider = new BGPGlobalProviderImpl(txChain, stateHolders);
    }

    @Test
    public void testCreateModuleKey() {
        assertEquals(new ModuleKey("instanceName", RibImpl.class), this.globalProvider.createModuleKey("instanceName"));
    }

    @Test
    public void testApply() {
        final Global global = this.globalProvider.apply(new BGPRibInstanceConfiguration(new InstanceConfigurationIdentifier("instanceName"), new AsNumber(1L),
                new Ipv4Address("1.2.3.4"), null,
                Lists.<BgpTableType>newArrayList(new BgpTableTypeImpl(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class))));
        final Global expectedGlobal = new GlobalBuilder()
            .setAfiSafis(
                    new AfiSafisBuilder().setAfiSafi(Collections.singletonList(new AfiSafiBuilder().setAfiSafiName(Linkstate.class).build())).build())
            .setConfig(new ConfigBuilder().setRouterId(new Ipv4Address("1.2.3.4")).setAs(new AsNumber(1L)).build())
            .build();
        assertEquals(expectedGlobal, global);
    }

    @Test
    public void testGetInstanceIdentifierString() {
        assertEquals(InstanceIdentifier.create(Bgp.class).child(Global.class), globalProvider.getInstanceIdentifier(null));
    }

    @Test
    public void testKeyForConfigurationGlobal() {
        final GlobalIdentifier globalId = (GlobalIdentifier) this.globalProvider.keyForConfiguration(new GlobalBuilder().build());
        assertEquals(GlobalIdentifier.GLOBAL_IDENTIFIER, globalId);
        assertEquals("GLOBAL", globalId.getName());
    }

    @Test
    public void testGetInstanceConfigurationType() {
        assertEquals(BGPRibInstanceConfiguration.class, this.globalProvider.getInstanceConfigurationType());
    }

}
