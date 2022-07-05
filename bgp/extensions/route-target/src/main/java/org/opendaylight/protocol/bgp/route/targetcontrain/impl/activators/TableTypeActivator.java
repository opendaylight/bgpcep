/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.ROUTETARGETCONSTRAIN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;

/**
 * Registers Route Target constrains Family type.
 *
 * @author Claudio D. Gasparini
 */
@Singleton
@Component(immediate = true,
           property = "type=org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators.TableTypeActivator")
@MetaInfServices
public final class TableTypeActivator implements BGPTableTypeRegistryProviderActivator {
    @Inject
    public TableTypeActivator() {
        // Exposed for DI
    }

    @Override
    public List<Registration> startBGPTableTypeRegistryProvider(final BGPTableTypeRegistryProvider provider) {
        return List.of(
            provider.registerBGPTableType(Ipv4AddressFamily.VALUE, RouteTargetConstrainSubsequentAddressFamily.VALUE,
                ROUTETARGETCONSTRAIN.class));
    }
}
