/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

public class FlowspecExtensionProviderContext {
    public enum AFI {
        IPV4(0),
        IPV6(1);

        public final int index;

        AFI(int i) {
            index = i;
        }
    }

    public enum SAFI {
        FLOWSPEC(0),
        FLOWSPEC_VPN(1);

        public final int index;

        SAFI(int i) {
            index = i;
        }
    }

    private final FlowspecTypeRegistry flowspecTypeRegistries[][] = new FlowspecTypeRegistry[2][2];

    public FlowspecExtensionProviderContext() {
        for (AFI afi : AFI.values()) {
            for (SAFI safi : SAFI.values()) {
                flowspecTypeRegistries[afi.index][safi.index] = new FlowspecTypeRegistry();
            }
        }
    }

    public FlowspecTypeRegistry getFlowspecTypeRegistry(AFI afi, SAFI safi) {
        return flowspecTypeRegistries[afi.index][safi.index];
    }
}
