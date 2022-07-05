/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import com.google.common.annotations.Beta;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * Utility class for dealing with Graceful Restart.
 */
public final class GracefulRestartUtil {
    /**
     * GR capability advertizing inactive GR.
     */
    public static final @NonNull GracefulRestartCapability EMPTY_GR_CAPABILITY = new GracefulRestartCapabilityBuilder()
            .setRestartFlags(new RestartFlags(Boolean.FALSE))
            .setRestartTime(Uint16.ZERO)
            .build();

    /**
     * LLGR capability advertizing no tables.
     */
    public static final @NonNull LlGracefulRestartCapability EMPTY_LLGR_CAPABILITY =
            new LlGracefulRestartCapabilityBuilder().build();

    private GracefulRestartUtil() {

    }

    @Beta
    public static @NonNull Tables gracefulRestartTable(final @NonNull AddressFamily afi,
            final @NonNull SubsequentAddressFamily safi, final boolean forwardingState) {
        return gracefulRestartTable(new TablesKey(afi, safi), forwardingState);
    }

    @Beta
    public static @NonNull Tables gracefulRestartTable(final @NonNull TablesKey table, final boolean forwardingState) {
        return new TablesBuilder().withKey(table).setAfiFlags(new AfiFlags(forwardingState)).build();
    }

    @Beta
    public static @NonNull CParameters gracefulRestartCapability(final Map<TablesKey, Tables> tables,
            final int restartTime, final boolean localRestarting) {
        return new CParametersBuilder().addAugmentation(new CParameters1Builder()
            .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                .setRestartFlags(new RestartFlags(localRestarting))
                .setRestartTime(Uint16.valueOf(restartTime))
                .setTables(tables)
                .build())
            .build()).build();
    }
}
