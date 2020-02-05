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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.McastVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * RIBActivator.
 *
 * @author Claudio D. Gasparini
 */
public final class RIBActivator extends AbstractRIBExtensionProviderActivator {
    @Override
    protected List<Registration> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context,
            final BindingNormalizedNodeSerializer mappingService) {
        return Lists.newArrayList(
                context.registerRIBSupport(Ipv4AddressFamily.class, McastVpnSubsequentAddressFamily.class,
                        MvpnIpv4RIBSupport.getInstance(mappingService)),
                context.registerRIBSupport(Ipv6AddressFamily.class, McastVpnSubsequentAddressFamily.class,
                        MvpnIpv6RIBSupport.getInstance(mappingService))
        );
    }
}
