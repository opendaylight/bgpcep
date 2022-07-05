/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProviderActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4MULTICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6MULTICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

/**
 * Registers L3VPN Family types.
 *
 * @author Claudio D. Gasparini
 */
@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.l3vpn.TableTypeActivator")
@MetaInfServices
public final class TableTypeActivator implements BGPTableTypeRegistryProviderActivator {
    @Inject
    public TableTypeActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> startBGPTableTypeRegistryProvider(final BGPTableTypeRegistryProvider provider) {
        return List.of(
            provider.registerBGPTableType(Ipv4AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE,
                L3VPNIPV4UNICAST.VALUE),
            provider.registerBGPTableType(Ipv6AddressFamily.VALUE, MplsLabeledVpnSubsequentAddressFamily.VALUE,
                L3VPNIPV6UNICAST.VALUE),
            provider.registerBGPTableType(Ipv4AddressFamily.VALUE, McastMplsLabeledVpnSubsequentAddressFamily.VALUE,
                L3VPNIPV4MULTICAST.VALUE),
            provider.registerBGPTableType(Ipv6AddressFamily.VALUE, McastMplsLabeledVpnSubsequentAddressFamily.VALUE,
                L3VPNIPV6MULTICAST.VALUE));
    }
}
