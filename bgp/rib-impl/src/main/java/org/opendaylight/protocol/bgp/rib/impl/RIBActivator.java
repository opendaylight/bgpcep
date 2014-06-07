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

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public final class RIBActivator extends AbstractRIBExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startRIBExtensionProviderImpl(final RIBExtensionProviderContext context) {
        AdjRIBsInFactory adj1 = new AdjRIBsInFactory() {
            @Override
            public AdjRIBsIn createAdjRIBsIn(final DataModificationTransaction trans, final RibReference rib, final TablesKey key) {
                return new Ipv4AdjRIBsIn(trans, rib, key);
            }
        };

        AdjRIBsInFactory adj2 = new AdjRIBsInFactory() {
            @Override
            public AdjRIBsIn createAdjRIBsIn(final DataModificationTransaction trans, final RibReference rib, final TablesKey key) {
                return new Ipv6AdjRIBsIn(trans, rib, key);
            }
        };
        return Lists.newArrayList(
                context.registerAdjRIBsInFactory(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, adj1),
                context.registerAdjRIBsInFactory(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, adj2));
    }
}
