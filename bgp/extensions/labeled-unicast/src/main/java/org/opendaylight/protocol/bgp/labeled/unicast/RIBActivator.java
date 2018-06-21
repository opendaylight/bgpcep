/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;

public class RIBActivator extends AbstractRIBExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startRIBExtensionProviderImpl(
        final RIBExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>(2);
        regs.add(context.registerRIBSupport(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, LabeledUnicastIpv4RIBSupport.getInstance()));
        regs.add(context.registerRIBSupport(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, LabeledUnicastIpv6RIBSupport.getInstance()));
        return regs;
    }
}
