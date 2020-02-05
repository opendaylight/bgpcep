/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators;

import java.util.Collections;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.spi.AbstractBGPTableTypeRegistryProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.ROUTETARGETCONSTRAIN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

/**
 * Registers Route Target constrains Family type.
 *
 * @author Claudio D. Gasparini
 */
public final class TableTypeActivator extends AbstractBGPTableTypeRegistryProviderActivator {
    @Override
    protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(
            final BGPTableTypeRegistryProvider provider) {
        return Collections.singletonList(
                provider.registerBGPTableType(Ipv4AddressFamily.class,
                        RouteTargetConstrainSubsequentAddressFamily.class, ROUTETARGETCONSTRAIN.class)
        );
    }
}