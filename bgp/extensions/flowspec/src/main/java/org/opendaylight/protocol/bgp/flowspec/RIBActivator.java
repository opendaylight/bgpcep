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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4RIBSupport;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6.FlowspecL3vpnIpv6RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.concepts.Registration;

public final class RIBActivator extends AbstractRIBExtensionProviderActivator {

    private final SimpleFlowspecExtensionProviderContext fsContext;

    public RIBActivator(SimpleFlowspecExtensionProviderContext context) {
        super();
        this.fsContext = context;
    }

    @Override
    protected List<Registration> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        final List<Registration> regs = new ArrayList<>(4);
        regs.add(context.registerRIBSupport(Ipv4AddressFamily.class, FlowspecSubsequentAddressFamily.class,
                FlowspecIpv4RIBSupport.getInstance(this.fsContext, mappingService)));
        regs.add(context.registerRIBSupport(Ipv6AddressFamily.class, FlowspecSubsequentAddressFamily.class,
                FlowspecIpv6RIBSupport.getInstance(this.fsContext, mappingService)));
        regs.add(context.registerRIBSupport(Ipv4AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class,
                FlowspecL3vpnIpv4RIBSupport.getInstance(this.fsContext, mappingService)));
        regs.add(context.registerRIBSupport(Ipv6AddressFamily.class, FlowspecL3vpnSubsequentAddressFamily.class,
                FlowspecL3vpnIpv6RIBSupport.getInstance(this.fsContext, mappingService)));
        return regs;
    }
}
