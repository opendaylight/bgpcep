/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv4;

import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.MplsLabeledVpnSubsequentAddressFamily;

public class RibIpv4Activator extends AbstractRIBExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startRIBExtensionProviderImpl(
            final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {

        return Collections.singletonList(context.registerRIBSupport(Ipv4AddressFamily.class,
                MplsLabeledVpnSubsequentAddressFamily.class, VpnIpv4RIBSupport.getInstance(mappingService)));
    }

}
