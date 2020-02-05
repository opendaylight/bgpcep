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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.concepts.Registration;

public class RIBActivator extends AbstractRIBExtensionProviderActivator {

    @Override
    protected List<Registration> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        final List<Registration> regs = new ArrayList<>(2);
        regs.add(context.registerRIBSupport(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                LabeledUnicastIpv4RIBSupport.getInstance(mappingService)));
        regs.add(context.registerRIBSupport(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                LabeledUnicastIpv6RIBSupport.getInstance(mappingService)));
        return regs;
    }
}
