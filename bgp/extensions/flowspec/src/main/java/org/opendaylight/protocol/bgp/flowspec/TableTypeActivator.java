/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.Arrays;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.spi.AbstractBGPTableTypeRegistryProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6FLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6L3VPNFLOW;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public final class TableTypeActivator extends AbstractBGPTableTypeRegistryProviderActivator {
    @Override
    protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
            final BGPTableTypeRegistryProvider provider) {
        return Arrays.asList(
            provider.registerBGPTableType(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class,
                IPV4FLOW.class),
            provider.registerBGPTableType(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class,
                IPV6FLOW.class),
            provider.registerBGPTableType(Ipv4AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class,
                IPV4L3VPNFLOW.class),
            provider.registerBGPTableType(Ipv6AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class,
                IPV6L3VPNFLOW.class));
    }
}
