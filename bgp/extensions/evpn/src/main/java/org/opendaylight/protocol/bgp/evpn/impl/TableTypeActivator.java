/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProviderActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L2VPNEVPN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.L2vpnAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.evpn.impl.TableTypeActivator")
@MetaInfServices
public final class TableTypeActivator implements BGPTableTypeRegistryProviderActivator {
    @Inject
    public TableTypeActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> startBGPTableTypeRegistryProvider(final BGPTableTypeRegistryProvider provider) {
        return List.of(provider.registerBGPTableType(L2vpnAddressFamily.VALUE, EvpnSubsequentAddressFamily.VALUE,
            L2VPNEVPN.VALUE));
    }
}
