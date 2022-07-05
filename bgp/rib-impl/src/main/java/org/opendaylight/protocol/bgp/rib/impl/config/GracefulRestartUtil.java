/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.parser.GracefulRestartUtil.gracefulRestartCapability;
import static org.opendaylight.protocol.bgp.parser.GracefulRestartUtil.gracefulRestartTable;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.BgpPeerUtil;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.GracefulRestart;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.graceful.restart.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.Config1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.ll.graceful.restart.rev181112.afi.safi.ll.graceful.restart.LlGracefulRestart;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers for dealing with Graceful Restart capabilities.
 *
 * @deprecated This class is competing with bgp-parser-api's view of GR and will be renamed and/or eliminated.
 */
// FIXME: This functionality should live in bgp-parser-api, except the use of RIB version of TablesKey prevents that.
//        We should be able to refactor this class by providing TablesKey translation somewhere.
@Deprecated
public final class GracefulRestartUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GracefulRestartUtil.class);

    private GracefulRestartUtil() {

    }

    public static CParameters getGracefulCapability(final Map<TablesKey, Boolean> tables,
                                                    final int restartTime,
                                                    final boolean localRestarting) {
        return gracefulRestartCapability(tables.entrySet().stream()
            .map(entry -> {
                final TablesKey key = entry.getKey();
                return gracefulRestartTable(key.getAfi(), key.getSafi(), entry.getValue());
            })
            .collect(Collectors.toMap(Identifiable::key, Function.identity())), restartTime, localRestarting);
    }

    public static CParameters getLlGracefulCapability(final Set<BgpPeerUtil.LlGracefulRestartDTO> llGracefulRestarts) {
        return new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setLlGracefulRestartCapability(new LlGracefulRestartCapabilityBuilder()
                        .setTables(llGracefulRestarts.stream()
                            .map(dto -> new TablesBuilder()
                                .setAfi(dto.getTableKey().getAfi())
                                .setSafi(dto.getTableKey().getSafi())
                                .setAfiFlags(new AfiFlags(dto.isForwarding()))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(dto.getStaleTime())))
                                .build())
                            .collect(Collectors.toMap(Tables::key, Function.identity())))
                        .build())
                    .build())
                .build();
    }

    static Set<TablesKey> getGracefulTables(final Collection<? extends AfiSafi> afiSafis,
                                            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        Set<TablesKey> gracefulTables = new HashSet<>();
        for (final AfiSafi afiSafi : afiSafis) {
            final GracefulRestart gr = afiSafi.getGracefulRestart();
            if (gr != null) {
                final Config config = gr.getConfig();
                if (config != null && Boolean.TRUE.equals(config.getEnabled())) {
                    final AfiSafiType afiSafiName = afiSafi.getAfiSafiName();
                    if (afiSafiName != null) {
                        final TablesKey tablesKey = tableTypeRegistry.getTableKey(afiSafiName);
                        if (tablesKey != null) {
                            gracefulTables.add(tablesKey);
                        }
                    }
                }
            }
        }
        return gracefulTables;
    }

    static Map<TablesKey, Integer> getLlGracefulTimers(final Collection<? extends AfiSafi> afiSafis,
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
                        if (config != null) {
                            final Uint24 staleTime = config.getLongLivedStaleTime();
                            if (staleTime != null && staleTime.getValue().toJava() > 0) {
                                final TablesKey key = tableTypeRegistry.getTableKey(afiSafi.getAfiSafiName());
                                if (key != null) {
                                    timers.put(key, staleTime.getValue().intValue());
                                } else {
                                    LOG.debug("Skipping unsupported afi-safi {}", afiSafi.getAfiSafiName());
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
        capabilities.add(new OptionalCapabilitiesBuilder()
            .setCParameters(getGracefulCapability(Maps.asMap(gracefulTables, preservedTables::contains),
                gracefulRestartTimer, localRestarting))
            .build());
        capabilities.add(new OptionalCapabilitiesBuilder()
            .setCParameters(getLlGracefulCapability(llGracefulDTOs))
            .build());

        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }
}
