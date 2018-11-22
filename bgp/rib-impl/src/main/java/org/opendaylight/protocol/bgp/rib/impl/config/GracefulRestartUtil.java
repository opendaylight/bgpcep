/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.BgpPeerUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.afi.safi.ll.graceful.restart.LlGracefulRestart;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GracefulRestartUtil {

    private static final Logger LOG = LoggerFactory.getLogger(GracefulRestartUtil.class);
    public static final GracefulRestartCapability EMPTY_GRACEFUL_CAPABILITY = new GracefulRestartCapabilityBuilder()
            .setRestartTime(0)
            .setTables(Collections.emptyList())
            .setRestartFlags(new GracefulRestartCapability.RestartFlags(false))
            .build();
    public static final LlGracefulRestartCapability EMPTY_LL_GRACEFUL_CAPABILITY =
            new LlGracefulRestartCapabilityBuilder()
                    .setTables(Collections.emptyList())
                    .build();

    private GracefulRestartUtil() {
        throw new UnsupportedOperationException();
    }

    public static CParameters getGracefulCapability(final Map<TablesKey, Boolean> tables,
                                                    final int restartTime,
                                                    final boolean localRestarting) {
        final List<Tables> tablesList = tables.entrySet().stream()
                .map(entry -> new TablesBuilder()
                        .setAfi(entry.getKey().getAfi())
                        .setSafi(entry.getKey().getSafi())
                        .setAfiFlags(new Tables.AfiFlags(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
       return new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                        .setRestartFlags(new GracefulRestartCapability.RestartFlags(localRestarting))
                        .setRestartTime(restartTime)
                        .setTables(tablesList)
               .build()).build()).build();
    }

    public static CParameters getLlGracefulCapability(final Set<BgpPeerUtil.LlGracefulRestartDTO> llGracefulRestarts) {
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables> tablesList =
                llGracefulRestarts.stream()
                .map(dto -> new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesBuilder()
                        .setAfi(dto.getTableKey().getAfi())
                        .setSafi(dto.getTableKey().getSafi())
                        .setAfiFlags(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables.AfiFlags(
                                        dto.isForwarding()))
                        .setLongLiveStaleTime((long) dto.getStaleTime())
                        .build())
                .collect(Collectors.toList());
        return new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(
                        new LlGracefulRestartCapabilityBuilder()
                        .setTables(tablesList)
                        .build()).build()).build();
    }

    static Set<TablesKey> getGracefulTables(final List<AfiSafi> afiSafis,
                                            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Set<TablesKey> gracefulTables = new HashSet<>();
        for (AfiSafi afiSafi : afiSafis) {
            if (afiSafi.getGracefulRestart() != null
                    && afiSafi.getGracefulRestart().getConfig() != null
                    && afiSafi.getGracefulRestart().getConfig().isEnabled()) {
                Class<? extends AfiSafiType> afiSafiName = afiSafi.getAfiSafiName();
                if (afiSafiName != null) {
                    Optional<TablesKey> tableKey = tableTypeRegistry.getTableKey(afiSafiName);
                    if (tableKey.isPresent()) {
                        TablesKey tablesKey = tableKey.get();
                        gracefulTables.add(tablesKey);
                    }
                }
            }
        }
        return gracefulTables;
    }

    static Map<TablesKey, Integer> getLlGracefulTimers(final List<AfiSafi> afiSafis,
                                                       final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        final Map<TablesKey, Integer> timers = new HashMap<>();
        afiSafis.forEach(afiSafi -> {
            final GracefulRestart gracefulRestart = afiSafi.getGracefulRestart();
            if (gracefulRestart != null) {
                final Config gracefulRestartConfig = gracefulRestart.getConfig();
                if (gracefulRestartConfig != null) {
                    final LlGracefulRestart llGracefulRestart;
                    final Config1 peerAug = gracefulRestartConfig.augmentation(Config1.class);
                    if (peerAug != null) {
                       llGracefulRestart = peerAug.getLlGracefulRestart();
                    } else {
                        final Config2 neighborAug = gracefulRestartConfig.augmentation(Config2.class);
                        if (neighborAug != null) {
                            llGracefulRestart = neighborAug.getLlGracefulRestart();
                        } else {
                            return;
                        }
                    }
                    if (llGracefulRestart != null) {
                        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart
                                .rev181112.afi.safi.ll.graceful.restart.ll.graceful.restart.Config config =
                                llGracefulRestart.getConfig();
                        if(config != null) {
                            final Long staleTime = config.getLongLiveStaleTime();
                            if (staleTime != null && staleTime > 0) {
                                final Optional<TablesKey> key = tableTypeRegistry.getTableKey(afiSafi.getAfiSafiName());
                                if (key.isPresent()) {
                                    timers.put(key.get(), staleTime.intValue());
                                } else {
                                    LOG.debug("Skipping unsupported afi-safi {}",afiSafi.getAfiSafiName());
                                }
                            }
                        }
                    }
                }
            }
        });
        return timers;
    }

    public static BgpParameters getGracefulBgpParameters(final List<OptionalCapabilities> fixedCapabilities,
                                                         final Set<TablesKey> gracefulTables,
                                                         final Set<TablesKey> preservedTables,
                                                         final int gracefulRestartTimer,
                                                         final boolean localRestarting,
                                                         final Set<BgpPeerUtil.LlGracefulRestartDTO> llGracefulDTOs) {
        final List<OptionalCapabilities> capabilities = new ArrayList<>(fixedCapabilities);
        final Map<TablesKey, Boolean> gracefulMap = new HashMap<>();
        gracefulTables.forEach(table -> gracefulMap.put(table, preservedTables.contains(table)));
        final CParameters gracefulCapability = getGracefulCapability(gracefulMap, gracefulRestartTimer,
                localRestarting);
        capabilities.add(new OptionalCapabilitiesBuilder().setCParameters(gracefulCapability).build());
        final CParameters llGracefulCapability = getLlGracefulCapability(llGracefulDTOs);
        capabilities.add(new OptionalCapabilitiesBuilder().setCParameters(llGracefulCapability).build());

        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }
}
