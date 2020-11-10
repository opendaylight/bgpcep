/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

public final class FlowspecTypeRegistries {
    public enum AFI {
        IPV4(),
        IPV6(),
    }

    public enum SAFI {
        FLOWSPEC,
        FLOWSPEC_VPN,
    }

    private static final FlowspecTypeRegistry[][] FLOWSPEC_TYPE_REGISTRIES;

    static {
        final FlowspecTypeRegistry[][] regs = new FlowspecTypeRegistry[AFI.values().length][SAFI.values().length];
        for (SAFI safi : SAFI.values()) {
            regs[AFI.IPV4.ordinal()][safi.ordinal()] = new FlowspecTypeRegistryBuilder()
                .registerIpv4FlowspecTypeHandlers().build();
            regs[AFI.IPV6.ordinal()][safi.ordinal()] = new FlowspecTypeRegistryBuilder()
                .registerIpv6FlowspecTypeHandlers().build();
        }
        FLOWSPEC_TYPE_REGISTRIES = regs;
    }

    private FlowspecTypeRegistries() {
        // Hidden on purpose
    }

    public static FlowspecTypeRegistry getFlowspecTypeRegistry(final AFI afi, final SAFI safi) {
        return FLOWSPEC_TYPE_REGISTRIES[afi.ordinal()][safi.ordinal()];
    }
}
