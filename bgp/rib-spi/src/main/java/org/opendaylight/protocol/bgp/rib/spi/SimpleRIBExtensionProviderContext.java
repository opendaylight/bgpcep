/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRIBExtensionProviderContext implements RIBExtensionProviderContext {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRIBExtensionProviderContext.class);

    private final ConcurrentMap<TablesKey, RIBSupport<?, ?>> supports = new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeIdentifierWithPredicates, RIBSupport<?, ?>> domSupports = new ConcurrentHashMap<>();

    @Override
    public <T extends RIBSupport<?, ?>> RIBSupportRegistration<T> registerRIBSupport(
            final AddressFamily afi, final SubsequentAddressFamily safi, final T support) {
        final TablesKey key = new TablesKey(afi, safi);
        final RIBSupport<?, ?> prev = supports.putIfAbsent(key, support);
        checkArgument(prev == null, "AFI %s SAFI %s is already registered with %s", afi, safi, prev);
        domSupports.put(RibSupportUtils.toYangTablesKey(afi, safi), support);
        return new AbstractRIBSupportRegistration<>(support) {
            @Override
            protected void removeRegistration() {
                // FIXME: clean up registrations, too
                supports.remove(key);
            }
        };
    }

    @Override
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>> RIBSupport<C, S> getRIBSupport(
            final AddressFamily afi, final SubsequentAddressFamily safi) {
        return getRIBSupport(new TablesKey(afi, safi));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>> RIBSupport<C, S> getRIBSupport(
            final TablesKey key) {
        return (RIBSupport<C, S>) supports.get(requireNonNull(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Routes & DataObject & ChoiceIn<Tables>, S extends ChildOf<C>> RIBSupport<C, S> getRIBSupport(
            final NodeIdentifierWithPredicates key) {
        return (RIBSupport<C, S>) domSupports.get(key);
    }
}
