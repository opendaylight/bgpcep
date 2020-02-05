/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4MULTICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6MULTICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

/**
 * Registers L3VPN Family types.
 *
 * @author Claudio D. Gasparini
 */
public final class TableTypeActivator extends AbstractBGPTableTypeRegistryProviderActivator {
    @Override
    protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
            final BGPTableTypeRegistryProvider provider) {
        return Lists.newArrayList(
                provider.registerBGPTableType(Ipv4AddressFamily.class,
                        MplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV4UNICAST.class),
                provider.registerBGPTableType(Ipv6AddressFamily.class,
                        MplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV6UNICAST.class),
                provider.registerBGPTableType(Ipv4AddressFamily.class,
                        McastMplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV4MULTICAST.class),
                provider.registerBGPTableType(Ipv6AddressFamily.class,
                        McastMplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV6MULTICAST.class)
        );
    }
}
