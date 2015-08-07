/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public class RIBActivator extends AbstractRIBExtensionProviderActivator {
    @Override
    protected List<AutoCloseable> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        regs.add(context.registerRIBSupport(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class, FlowspecIpv4RIBSupport.getInstance()));
        regs.add(context.registerRIBSupport(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class, FlowspecIpv6RIBSupport.getInstance()));
        return regs;
    }
}
