/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public class SimpleRIBExtensionProviderContext implements RIBExtensionProviderContext {
    private final Map<TablesKey, AdjRIBsFactory> factories = new ConcurrentHashMap<>();

    @Override
    public final synchronized AbstractRegistration registerAdjRIBsInFactory(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final AdjRIBsFactory factory) {
        final TablesKey key = new TablesKey(afi, safi);

        if (this.factories.containsKey(key)) {
            throw new IllegalArgumentException("Specified AFI/SAFI combination is already registered");
        }

        this.factories.put(key, factory);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleRIBExtensionProviderContext.this.factories.remove(key);
                }
            }
        };
    }

    @Override
    public final synchronized AdjRIBsFactory getAdjRIBsInFactory(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi) {
        return this.factories.get(new TablesKey(afi, safi));
    }
}
