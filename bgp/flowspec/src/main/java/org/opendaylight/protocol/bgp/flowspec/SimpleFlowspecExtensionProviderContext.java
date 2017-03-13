/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

public class SimpleFlowspecExtensionProviderContext {
    public enum AFI {
        IPV4(0),
        IPV6(1);

        public final int index;

        AFI(int i) {
            this.index = i;
        }
    }

    public enum SAFI {
        FLOWSPEC(0),
        FLOWSPEC_VPN(1);

        public final int index;

        SAFI(int i) {
            this.index = i;
        }
    }

    private final SimpleFlowspecTypeRegistry flowspecTypeRegistries[][] = new SimpleFlowspecTypeRegistry[2][2];

    public SimpleFlowspecExtensionProviderContext() {
        for (AFI afi : AFI.values()) {
            for (SAFI safi : SAFI.values()) {
                this.flowspecTypeRegistries[afi.index][safi.index] = new SimpleFlowspecTypeRegistry();
            }
        }
    }

    public SimpleFlowspecTypeRegistry getFlowspecTypeRegistry(AFI afi, SAFI safi) {
        return this.flowspecTypeRegistries[afi.index][safi.index];
    }
}
