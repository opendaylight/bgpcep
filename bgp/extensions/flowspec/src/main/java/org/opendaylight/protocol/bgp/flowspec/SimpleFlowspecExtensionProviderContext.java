/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

public final class SimpleFlowspecExtensionProviderContext {
    public enum AFI {
        IPV4(0b0),
        IPV6(0b1);

        final int index;

        AFI(final int index) {
            this.index = index;
        }
    }

    public enum SAFI {
        FLOWSPEC(0b00),
        FLOWSPEC_VPN(0b10);

        final int index;

        SAFI(final int index) {
            this.index = index;
        }
    }

    private final SimpleFlowspecTypeRegistry[] flowspecTypeRegistries;

    public SimpleFlowspecExtensionProviderContext() {
        final int size = AFI.values().length * SAFI.values().length;
        flowspecTypeRegistries = new SimpleFlowspecTypeRegistry[size];
        for (int i = 0; i < size; ++i) {
            flowspecTypeRegistries[i] = new SimpleFlowspecTypeRegistry();
        }
    }

    public FlowspecTypeRegistry getFlowspecTypeRegistry(final AFI afi, final SAFI safi) {
        return flowspecTypeRegistry(afi, safi);
    }

    SimpleFlowspecTypeRegistry flowspecTypeRegistry(final AFI afi, final SAFI safi) {
        return this.flowspecTypeRegistries[afi.index + safi.index];
    }
}
