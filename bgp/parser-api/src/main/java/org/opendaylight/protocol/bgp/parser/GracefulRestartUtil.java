/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;

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
}
