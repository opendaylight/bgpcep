/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.spi.AbstractBGPTableTypeRegistryProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public final class TableTypeActivator extends AbstractBGPTableTypeRegistryProviderActivator {

    @Override
    protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
            final BGPTableTypeRegistryProvider provider) {
        return Lists.newArrayList(
                provider.registerBGPTableType(Ipv4AddressFamily.class,
                        MplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV4UNICAST.class),
                provider.registerBGPTableType(Ipv6AddressFamily.class,
                        MplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV6UNICAST.class));
    }

}
