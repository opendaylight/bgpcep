/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.Preconditions;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

public class SimpleRIBExtensionProviderContext implements RIBExtensionProviderContext {
    private final ConcurrentMap<TablesKey, AdjRIBsFactory> factories = new ConcurrentHashMap<>();
    private final ConcurrentMap<TablesKey, RIBSupport> supports = new ConcurrentHashMap<>();

    @Override
    public final synchronized AbstractRegistration registerAdjRIBsInFactory(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final AdjRIBsFactory factory) {
        final TablesKey key = new TablesKey(afi, safi);

        if (this.factories.containsKey(key)) {
            throw new IllegalArgumentException("Specified AFI/SAFI combination is already registered");
        }

        this.factories.put(key, factory);

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (SimpleRIBExtensionProviderContext.this) {
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

    @Override
    public <T extends RIBSupport> RIBSupportRegistration<T> registerRIBSupport(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final T support) {
        final TablesKey key = new TablesKey(afi, safi);

        final RIBSupport prev = this.supports.putIfAbsent(key, support);
        Preconditions.checkArgument(prev == null, "AFI %s SAFI %s is already registered with %s", afi, safi, prev);

        return new AbstractRIBSupportRegistration<T>(support) {
            @Override
            protected void removeRegistration() {
                SimpleRIBExtensionProviderContext.this.supports.remove(key);
            }
        };
    }

    @Override
    public RIBSupport getRIBSupport(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        return getRIBSupport(new TablesKey(afi, safi));
    }

    @Override
    public RIBSupport getRIBSupport(final TablesKey key) {
        return this.supports.get(Preconditions.checkNotNull(key));
    }
}
