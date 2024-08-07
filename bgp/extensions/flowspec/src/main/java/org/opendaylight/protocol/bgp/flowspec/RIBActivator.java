/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4RIBSupport;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6.FlowspecL3vpnIpv6RIBSupport;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

@Singleton
@Component(immediate = true, property = "type=org.opendaylight.protocol.bgp.flowspec.RIBActivator")
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
            context.registerRIBSupport(new FlowspecIpv4RIBSupport(mappingService)),
            context.registerRIBSupport(new FlowspecIpv6RIBSupport(mappingService)),
            context.registerRIBSupport(new FlowspecL3vpnIpv4RIBSupport(mappingService)),
            context.registerRIBSupport(new FlowspecL3vpnIpv6RIBSupport(mappingService)));
    }
}
