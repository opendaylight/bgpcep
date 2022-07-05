/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.protocol.bgp.rib.impl.BgpPeerUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.Config1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.afi.safi.ll.graceful.restart.LlGracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class GracefulRestartUtilTest {

    private static final int RESTART_TIME = 5;
    private static final int STALE_TIME = 10;
    private static final boolean RESTARTING = true;
    private static final AfiSafi IPV4_UNICAST_AFISAFI = new AfiSafiBuilder()
            .setAfiSafiName(IPV4UNICAST.VALUE)
            .setGracefulRestart(new GracefulRestartBuilder()
                    .setConfig(new ConfigBuilder()
                            .setEnabled(true)
                            .build()).build()).build();
    private static final AfiSafi IPV4_MULTICAST_AFISAFI = new AfiSafiBuilder()
            .setAfiSafiName(IPV4LABELLEDUNICAST.VALUE)
            .setGracefulRestart(new GracefulRestartBuilder()
                    .setConfig(new ConfigBuilder()
                            .setEnabled(false)
                            .build()).build()).build();
    private static final AfiSafi IPV6_AFISAFI = new AfiSafiBuilder()
            .setAfiSafiName(IPV6UNICAST.VALUE)
            .setGracefulRestart(new GracefulRestartBuilder()
                    .setConfig(new ConfigBuilder()
                            .setEnabled(true)
                            .build()).build()).build();
    private static final TablesKey IPV4_KEY = new TablesKey(Ipv4AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE);
    private static final TablesKey IPV6_KEY = new TablesKey(Ipv6AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE);

    private static final List<AfiSafi> AFISAFIS = ImmutableList.of(IPV4_UNICAST_AFISAFI, IPV4_MULTICAST_AFISAFI,
        IPV6_AFISAFI);

    @Mock
    private BGPTableTypeRegistryConsumer tableRegistry;


    @Before
    public void setUp() {
        doReturn(IPV4_KEY).when(tableRegistry).getTableKey(IPV4UNICAST.VALUE);
        doReturn(IPV6_KEY).when(tableRegistry).getTableKey(IPV6UNICAST.VALUE);
    }

    @Test
    public void getGracefulCapabilityTest() {
        final Map<TablesKey, Boolean> gracefulMap = new HashMap<>();
        gracefulMap.put(IPV4_KEY, true);
        gracefulMap.put(IPV6_KEY, false);
        CParameters capability = GracefulRestartUtil.getGracefulCapability(gracefulMap, RESTART_TIME, RESTARTING);
        final CParameters1 params = capability.augmentation(CParameters1.class);
        assertNotNull(params);
        final GracefulRestartCapability gracefulCapability = params.getGracefulRestartCapability();
        assertNotNull(gracefulCapability);
        assertTrue(gracefulCapability.getRestartFlags().getRestartState());
        assertEquals(RESTART_TIME, gracefulCapability.getRestartTime().intValue());
        final var tables = gracefulCapability.getTables();
        assertNotNull(tables);
        assertEquals(2, tables.size());
        tables.values().forEach(table -> {
            assertTrue(isSameKey(IPV4_KEY, table.key()) && table.getAfiFlags().getForwardingState()
                || isSameKey(IPV6_KEY, table.key()) && !table.getAfiFlags().getForwardingState());
        });
    }

    @Test
    public void getGracefulTablesTest() {
        final Set<TablesKey> gracefulTables = GracefulRestartUtil.getGracefulTables(AFISAFIS, tableRegistry);
        assertEquals(2, gracefulTables.size());
        assertTrue(gracefulTables.contains(IPV4_KEY));
        assertTrue(gracefulTables.contains(IPV6_KEY));
    }

    @Test
    public void getGracefulBgpParametersTest() {
        final OptionalCapabilities cap1 = new OptionalCapabilitiesBuilder()
                .setCParameters(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build();
        final OptionalCapabilities cap2 = new OptionalCapabilitiesBuilder()
                .setCParameters(MultiprotocolCapabilitiesUtil.RR_CAPABILITY).build();
        final List<OptionalCapabilities> fixedCaps = new ArrayList<>();
        fixedCaps.add(cap1);
        fixedCaps.add(cap2);
        final Set<TablesKey> gracefulTables = new HashSet<>();
        gracefulTables.add(IPV4_KEY);
        gracefulTables.add(IPV6_KEY);
        final Set<TablesKey> preservedTables = new HashSet<>();
        preservedTables.add(IPV4_KEY);

        final Map<TablesKey, Boolean> gracefulMap = new HashMap<>();
        gracefulMap.put(IPV4_KEY, true);
        gracefulMap.put(IPV6_KEY, false);
        final OptionalCapabilities expectedGracefulCapability = new OptionalCapabilitiesBuilder()
                .setCParameters(GracefulRestartUtil.getGracefulCapability(gracefulMap, RESTART_TIME, RESTARTING))
                .build();
        final Set<BgpPeerUtil.LlGracefulRestartDTO> llGracefulDTOs = Collections.singleton(
                new BgpPeerUtil.LlGracefulRestartDTO(IPV4_KEY, STALE_TIME, true));
        final OptionalCapabilities expectedLlGracefulCapability = new OptionalCapabilitiesBuilder()
                .setCParameters(GracefulRestartUtil.getLlGracefulCapability(llGracefulDTOs))
                .build();
        final BgpParameters parameters = GracefulRestartUtil.getGracefulBgpParameters(fixedCaps, gracefulTables,
                preservedTables, RESTART_TIME, RESTARTING, llGracefulDTOs);
        final List<OptionalCapabilities> capabilities = parameters.getOptionalCapabilities();
        assertTrue(capabilities != null);
        assertEquals(4, capabilities.size());
        assertTrue(capabilities.contains(cap1));
        assertTrue(capabilities.contains(cap2));
        assertTrue(capabilities.contains(expectedGracefulCapability));
        assertTrue(capabilities.contains(expectedLlGracefulCapability));
    }

    @Test
    public void getLlGracefulCapabilityTest() {
        final Set<BgpPeerUtil.LlGracefulRestartDTO> llGracefulRestartDTOs = new HashSet<>();
        llGracefulRestartDTOs.add(new BgpPeerUtil.LlGracefulRestartDTO(IPV4_KEY, STALE_TIME, true));
        llGracefulRestartDTOs.add(new BgpPeerUtil.LlGracefulRestartDTO(IPV6_KEY, STALE_TIME, false));
        CParameters capability = GracefulRestartUtil.getLlGracefulCapability(llGracefulRestartDTOs);
        final CParameters1 params = capability.augmentation(CParameters1.class);
        assertNotNull(params);
        final LlGracefulRestartCapability llGracefulCapability = params.getLlGracefulRestartCapability();
        assertNotNull(llGracefulCapability);
        final var tables = llGracefulCapability.getTables();
        assertNotNull(tables);
        assertEquals(2, tables.size());
        assertEquals(STALE_TIME, tables.values().iterator().next().getLongLivedStaleTime().getValue().intValue());
        tables.values().forEach(table -> {
            assertTrue(isSameKey(IPV4_KEY, table.key()) && table.getAfiFlags().getForwardingState()
                || isSameKey(IPV6_KEY, table.key()) && !table.getAfiFlags().getForwardingState());
        });
    }

    @Test
    public void getLlGracefulTimersTest() {
        final List<AfiSafi> afiSafi = new ArrayList<>();
        afiSafi.add(new AfiSafiBuilder()
            .setAfiSafiName(IPV4UNICAST.VALUE)
            .setGracefulRestart(new GracefulRestartBuilder()
                .setConfig(new ConfigBuilder()
                    .addAugmentation(new Config1Builder()
                        .setLlGracefulRestart(new LlGracefulRestartBuilder()
                            .setConfig(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll
                                .graceful.restart.rev181112.afi.safi.ll.graceful.restart.ll.graceful.restart
                                .ConfigBuilder().setLongLivedStaleTime(new Uint24(Uint32.valueOf(STALE_TIME))).build())
                            .build())
                        .build())
                    .build())
                .build())
            .build());
        final Map<TablesKey, Integer> llGracefulTimers = GracefulRestartUtil.getLlGracefulTimers(afiSafi,
            tableRegistry);
        assertNotNull(llGracefulTimers);
        assertEquals(1, llGracefulTimers.size());
        assertEquals(STALE_TIME, llGracefulTimers.get(IPV4_KEY).intValue());
    }

    private static boolean isSameKey(final TablesKey key1, final org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesKey key2) {
        return key1.getAfi() == key2.getAfi() && key1.getSafi() == key2.getSafi();
    }

    private static boolean isSameKey(final TablesKey key1, final org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesKey key2) {
        return key1.getAfi() == key2.getAfi() && key1.getSafi() == key2.getSafi();
    }
}
