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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

public final class GracefulRestartUtil {

    public static final GracefulRestartCapability EMPTY_GRACEFUL_CAPABILITY = new GracefulRestartCapabilityBuilder()
            .setRestartTime(0)
            .setTables(Collections.emptyList())
            .setRestartFlags(new GracefulRestartCapability.RestartFlags(false))
            .build();

    private GracefulRestartUtil() {
        throw new UnsupportedOperationException();
    }

    static CParameters getGracefulCapability(final Map<TablesKey, Boolean> tables,
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

    static Set<TablesKey> getGracefulTables(final List<AfiSafi> afiSafis,
                                            final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        return afiSafis.stream()
                .filter(afiSafi -> afiSafi.getGracefulRestart() != null)
                .filter(afiSafi -> afiSafi.getGracefulRestart().getConfig() != null)
                .filter(afiSafi -> afiSafi.getGracefulRestart().getConfig().isEnabled())
                .map(AfiSafi::getAfiSafiName)
                .filter(Objects::nonNull)
                .map(tableTypeRegistry::getTableKey)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public static BgpParameters getGracefulBgpParameters(final List<OptionalCapabilities> fixedCapabilities,
                                                         final Set<TablesKey> gracefulTables,
                                                         final Set<TablesKey> preservedTables,
                                                         final int gracefulRestartTimer,
                                                         final boolean localRestarting) {
        final List<OptionalCapabilities> capabilities = new ArrayList<>(fixedCapabilities);
        final Map<TablesKey, Boolean> gracefulMap = new HashMap<>();
        gracefulTables.forEach(table -> gracefulMap.put(table, preservedTables.contains(table)));
        final CParameters gracefulCapability = getGracefulCapability(gracefulMap,
                gracefulRestartTimer, localRestarting);
        capabilities.add(new OptionalCapabilitiesBuilder().setCParameters(gracefulCapability).build());

        return new BgpParametersBuilder().setOptionalCapabilities(capabilities).build();
    }
}
