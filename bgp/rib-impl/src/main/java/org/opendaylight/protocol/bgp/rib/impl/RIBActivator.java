/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.collect.Lists;

import java.util.List;

import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public final class RIBActivator extends AbstractRIBExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context) {
        AdjRIBsInFactory adj1 = new AdjRIBsInFactory() {
            @Override
            public AdjRIBsIn createAdjRIBsIn(final KeyedInstanceIdentifier<Tables, TablesKey> basePath, final AsNumber localAs) {
                return new Ipv4AdjRIBsIn(basePath, localAs);
            }
        };

        AdjRIBsInFactory adj2 = new AdjRIBsInFactory() {
            @Override
            public AdjRIBsIn createAdjRIBsIn(final KeyedInstanceIdentifier<Tables, TablesKey> basePath, final AsNumber localAs) {
                return new Ipv6AdjRIBsIn(basePath, localAs);
            }
        };
        return Lists.newArrayList(
                context.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, adj1),
                context.registerAdjRIBsInFactory(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, adj2));
    }
}
