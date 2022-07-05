/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Mutable;
import org.opendaylight.yangtools.concepts.Registration;

@VisibleForTesting
final class SimpleBGPTableTypeRegistryProvider extends AbstractBGPTableTypeRegistryConsumer
        implements BGPTableTypeRegistryProvider, Mutable {
    private final @NonNull BiMap<BgpTableType, Class<? extends AfiSafiType>> tableTypes = HashBiMap.create();
    private final @NonNull BiMap<TablesKey, Class<? extends AfiSafiType>> tableKeys = HashBiMap.create();

    @Override
    public Registration registerBGPTableType(final AddressFamily afi, final SubsequentAddressFamily safi,
            final Class<? extends AfiSafiType> afiSafiType) {
        final BgpTableType tableType = new BgpTableTypeImpl(afi, safi);
        final Class<? extends AfiSafiType> prev = tableTypes.putIfAbsent(tableType, afiSafiType);
        checkState(prev == null, "AFI %s SAFI %s is already registered with %s", afi, safi, prev);
        final TablesKey tableKey = new TablesKey(afi, safi);
        tableKeys.put(tableKey, afiSafiType);

        // For completeness, we do not really want this to happen
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                tableTypes.remove(tableType);
                tableKeys.remove(tableKey);
            }
        };
    }

    @Override
    BiMap<BgpTableType, Class<? extends AfiSafiType>> tableTypes() {
        return tableTypes;
    }

    @Override
    BiMap<TablesKey, Class<? extends AfiSafiType>> tableKeys() {
        return tableKeys;
    }
}