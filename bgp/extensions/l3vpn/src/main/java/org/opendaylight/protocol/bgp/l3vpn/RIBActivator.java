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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.l3vpn.mcast.L3VpnMcastIpv4RIBSupport;
import org.opendaylight.protocol.bgp.l3vpn.mcast.L3VpnMcastIpv6RIBSupport;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4.VpnIpv4RIBSupport;
import org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6.VpnIpv6RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.l3vpn.mcast.rev180417.McastMplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

/**
 * RIBActivator.
 *
 * @author Claudio D. Gasparini
 */
@Singleton
@Component(immediate = true, service = RIBExtensionProviderActivator.class,
           property = "type=org.opendaylight.protocol.bgp.l3vpn.RIBActivator")
@MetaInfServices(value = RIBExtensionProviderActivator.class)
public final class RIBActivator extends AbstractRIBExtensionProviderActivator {
    @Inject
    public RIBActivator() {
        // Exposed for DI
    }

    @Override
    protected List<Registration> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        return List.of(
            context.registerRIBSupport(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
                VpnIpv4RIBSupport.getInstance(mappingService)),
            context.registerRIBSupport(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
                VpnIpv6RIBSupport.getInstance(mappingService)),
            context.registerRIBSupport(Ipv4AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class,
                L3VpnMcastIpv4RIBSupport.getInstance(mappingService)),
            context.registerRIBSupport(Ipv6AddressFamily.class, McastMplsLabeledVpnSubsequentAddressFamily.class,
                L3VpnMcastIpv6RIBSupport.getInstance(mappingService)));
    }
}
