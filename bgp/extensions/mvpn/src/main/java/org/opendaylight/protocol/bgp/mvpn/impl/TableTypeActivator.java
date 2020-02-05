/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.spi.AbstractBGPTableTypeRegistryProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV4MCASTVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.IPV6MCASTVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

/**
 * Registers MCast VPN Family types.
 *
 * @author Claudio D. Gasparini
 */
public final class TableTypeActivator extends AbstractBGPTableTypeRegistryProviderActivator {
    @Override
    protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
            final BGPTableTypeRegistryProvider provider) {
        return Lists.newArrayList(
                provider.registerBGPTableType(
                        Ipv4AddressFamily.class, McastVpnSubsequentAddressFamily.class, IPV4MCASTVPN.class),
                provider.registerBGPTableType(
                        Ipv6AddressFamily.class, McastVpnSubsequentAddressFamily.class, IPV6MCASTVPN.class)
        );
    }
}
