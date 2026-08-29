/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class SimpleRIBExtensionProviderContext implements RIBExtensionProviderContext {
    private final ConcurrentHashMap<NodeIdentifierWithPredicates, RIBSupport<?, ?>> domSupports =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TablesKey, RIBSupport<?, ?>> supports = new ConcurrentHashMap<>();

    @Override
    public Registration registerRIBSupport(final RIBSupport<?, ?> support) {
        final var bindingKey = support.getTablesKey();
        final var prevBinding = supports.putIfAbsent(bindingKey, support);
        if (prevBinding != null) {
            throw new IllegalStateException(bindingKey + " is already registered with " + prevBinding);
        }

        final var domKey = support.tablesKey();
        final var prevDom = domSupports.putIfAbsent(domKey, support);
        if (prevDom != null) {
            throw new IllegalStateException(domKey + " is already registered with " + prevDom);
        }

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                // FIXME: clean up registrations, too
                supports.remove(bindingKey, support);
                domSupports.remove(domKey, support);
            }
        };
    }

    @Override
    public RIBSupport<?, ?> getRIBSupport(final AddressFamily afi, final SubsequentAddressFamily safi) {
        return getRIBSupport(new TablesKey(afi, safi));
    }

    @Override
    public RIBSupport<?, ?> getRIBSupport(final TablesKey key) {
        return supports.get(requireNonNull(key));
    }

    @Override
    public RIBSupport<?, ?> getRIBSupport(final NodeIdentifierWithPredicates key) {
        return domSupports.get(key);
    }
}
