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
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.MultiprotocolCapabilitiesUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestartBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;

public class GracefulRestartUtilTest {

    private static final int RESTART_TIME = 5;
    private static final boolean RESTARTING = true;
    private static final AfiSafi IPV4_UNICAST_AFISAFI = new AfiSafiBuilder()
            .setAfiSafiName(IPV4UNICAST.class)
            .setGracefulRestart(new GracefulRestartBuilder()
                    .setConfig(new ConfigBuilder()
                            .setEnabled(true)
                            .build()).build()).build();
    private static final AfiSafi IPV4_MULTICAST_AFISAFI = new AfiSafiBuilder()
            .setAfiSafiName(IPV4LABELLEDUNICAST.class)
            .setGracefulRestart(new GracefulRestartBuilder()
                    .setConfig(new ConfigBuilder()
                            .setEnabled(false)
                            .build()).build()).build();
    private static final AfiSafi IPV6_AFISAFI = new AfiSafiBuilder()
            .setAfiSafiName(IPV6UNICAST.class)
            .setGracefulRestart(new GracefulRestartBuilder()
                    .setConfig(new ConfigBuilder()
                            .setEnabled(true)
                            .build()).build()).build();
    private static final TablesKey IPV4_KEY = new TablesKey(Ipv4AddressFamily.class,
            UnicastSubsequentAddressFamily.class);
    private static final TablesKey IPV6_KEY = new TablesKey(Ipv6AddressFamily.class,
            UnicastSubsequentAddressFamily.class);private static final List<AfiSafi> AFISAFIS = new ArrayList<>();
    static {
        AFISAFIS.add(IPV4_UNICAST_AFISAFI);
        AFISAFIS.add(IPV4_MULTICAST_AFISAFI);
        AFISAFIS.add(IPV6_AFISAFI);
        }

    @Mock
    private BGPTableTypeRegistryConsumer tableRegistry;


    @Before
    public void setUp() {
        initMocks(this);
        doReturn(Optional.of(IPV4_KEY)).when(tableRegistry).getTableKey(IPV4UNICAST.class);
        doReturn(Optional.of(IPV6_KEY)).when(tableRegistry).getTableKey(IPV6UNICAST.class);
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
        assertTrue(gracefulCapability.getRestartFlags().isRestartState());
        assertEquals(RESTART_TIME, gracefulCapability.getRestartTime().intValue());
        final List<Tables> tables = gracefulCapability.getTables();
        assertNotNull(tables);
        assertEquals(2, tables.size());
        tables.forEach(table -> {
            assertTrue((isSameKey(IPV4_KEY, table.key()) && table.getAfiFlags().isForwardingState()) ||
                    (isSameKey(IPV6_KEY, table.key()) && !table.getAfiFlags().isForwardingState()));
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
        final BgpParameters parameters = GracefulRestartUtil.getGracefulBgpParameters(fixedCaps, gracefulTables,
                preservedTables, RESTART_TIME, RESTARTING);
        final List<OptionalCapabilities> capabilities = parameters.getOptionalCapabilities();
        assertTrue(capabilities != null);
        assertEquals(3, capabilities.size());
        assertTrue(capabilities.contains(cap1));
        assertTrue(capabilities.contains(cap2));
        assertTrue(capabilities.contains(expectedGracefulCapability));

    }

    private static boolean isSameKey(TablesKey key1,
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesKey key2) {
        return key1.getAfi() == key2.getAfi() &&
                key1.getSafi() == key2.getSafi();
    }
}
