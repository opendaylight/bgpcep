/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;

/**
 * Utility class for dealing with Graceful Restart.
 */
public final class GracefulRestartUtil {
    /**
     * GR capability advertizing inactive GR.
     */
    public static final GracefulRestartCapability EMPTY_GR_CAPABILITY = new GracefulRestartCapabilityBuilder()
            .setTables(ImmutableList.of())
            .setRestartFlags(new RestartFlags(Boolean.FALSE))
            .setRestartTime(0)
            .build();

    /**
     * LLGR capability advertizing no tables.
     */
    public static final LlGracefulRestartCapability EMPTY_LLGR_CAPABILITY = new LlGracefulRestartCapabilityBuilder()
            .setTables(ImmutableList.of())
            .build();

    private GracefulRestartUtil() {

    }

    public static Tables gracefulRestartTable(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final boolean forwardingState) {
        return new TablesBuilder().setAfi(afi).setSafi(safi).setAfiFlags(new AfiFlags(forwardingState)).build();
    }

    public static CParameters gracefulRestartCapability(final List<Tables> tables, final int restartTime,
            final boolean localRestarting) {
        return new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                .setRestartFlags(new RestartFlags(localRestarting))
                .setRestartTime(restartTime)
                .setTables(tables)
                .build())
            .build()).build();
    }
}
