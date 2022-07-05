/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

/**
 * RIBActivator.
 *
 * @author Claudio D. Gasparini
 */
@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.mvpn.impl.RIBActivator")
@MetaInfServices
public final class RIBActivator implements RIBExtensionProviderActivator {
    @Inject
    public RIBActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> startRIBExtensionProvider(final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        return List.of(
            context.registerRIBSupport(Ipv4AddressFamily.VALUE, McastVpnSubsequentAddressFamily.VALUE,
                new MvpnIpv4RIBSupport(mappingService)),
            context.registerRIBSupport(Ipv6AddressFamily.VALUE, McastVpnSubsequentAddressFamily.VALUE,
                new MvpnIpv6RIBSupport(mappingService)));
    }
}
