/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;

public final class GracefulCapabilityUtil {

    public static final GracefulRestartCapability EMPTY_GRACEFUL_CAPABILITY = new GracefulRestartCapabilityBuilder()
            .setRestartTime(0)
            .setTables(Collections.EMPTY_LIST)
            .setRestartFlags(new GracefulRestartCapability.RestartFlags(false))
            .build();

    private GracefulCapabilityUtil() {
        throw new UnsupportedOperationException();
    }

    static CParameters getGracefulCapability(final List<AfiSafi> afiSafis,
                                                    final int restartTime,
                                                    final boolean localRestarting,
                                                    final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        List<Tables> tablesList;
        if (restartTime == 0) {
            tablesList = Collections.EMPTY_LIST;
        } else {
            tablesList = afiSafis.stream()
                    .filter(afiSafi -> afiSafi.getGracefulRestart() != null)
                    .filter(afiSafi -> afiSafi.getGracefulRestart().getConfig() != null)
                    .filter(afiSafi -> afiSafi.getGracefulRestart().getConfig().isEnabled())
                    .map(AfiSafi::getAfiSafiName)
                    .filter(Objects::nonNull)
                    .map(tableTypeRegistry::getTableKey)
                    .filter(Optional::isPresent)
                    .map(tableKey -> new TablesBuilder()
                            .setAfi(tableKey.get().getAfi())
                            .setSafi(tableKey.get().getSafi())
                            .setAfiFlags(new Tables.AfiFlags(true))
                            .build())
                    .collect(Collectors.toList());
        }
       return new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                        .setRestartFlags(new GracefulRestartCapability.RestartFlags(localRestarting))
                        .setRestartTime(restartTime)
                        .setTables(tablesList)
               .build()).build()).build();
    }
}
