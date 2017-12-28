/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public final class SimpleBGPTableTypeRegistryProvider implements BGPTableTypeRegistryProvider {

    @GuardedBy("this")
    private final BiMap<BgpTableType, Class<? extends AfiSafiType>> tableTypes = HashBiMap.create();
    @GuardedBy("this")
    private final BiMap<TablesKey, Class<? extends AfiSafiType>> tableKeys = HashBiMap.create();

    @Override
    public synchronized AbstractRegistration registerBGPTableType(final Class<? extends AddressFamily> afi,
            final Class<? extends SubsequentAddressFamily> safi, final Class<? extends AfiSafiType> afiSafiType) {
        final BgpTableType tableType = new BgpTableTypeImpl(afi, safi);
        final Class<? extends AfiSafiType> prev = this.tableTypes.putIfAbsent(tableType, afiSafiType);
        Preconditions.checkState(prev == null, "AFI %s SAFI %s is already registered with %s",
                afi, safi, prev);
        final TablesKey tableKey = new TablesKey(tableType.getAfi(), tableType.getSafi());
        this.tableKeys.put(tableKey, afiSafiType);

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (SimpleBGPTableTypeRegistryProvider.this) {
                    SimpleBGPTableTypeRegistryProvider.this.tableTypes.remove(tableType);
                    SimpleBGPTableTypeRegistryProvider.this.tableKeys.remove(tableKey);
                }
            }
        };
    }

    @Override
    public synchronized Optional<BgpTableType> getTableType(final Class<? extends AfiSafiType> afiSafiType) {
        final BgpTableType tableType = this.tableTypes.inverse().get(afiSafiType);
        return Optional.ofNullable(tableType);
    }

    @Nonnull
    @Override
    public Optional<TablesKey> getTableKey(@Nonnull final Class<? extends AfiSafiType> afiSafiType) {
        final TablesKey tableKey = this.tableKeys.inverse().get(afiSafiType);
        return Optional.ofNullable(tableKey);
    }

    @Override
    public synchronized Optional<Class<? extends AfiSafiType>> getAfiSafiType(final BgpTableType bgpTableType) {
        final Class<? extends AfiSafiType> afiSafi = this.tableTypes.get(bgpTableType);
        return Optional.ofNullable(afiSafi);
    }

    @Nonnull
    @Override
    public Optional<Class<? extends AfiSafiType>> getAfiSafiType(@Nonnull final TablesKey tablesKey) {
        final Class<? extends AfiSafiType> afiSafi = this.tableKeys.get(tablesKey);
        return Optional.ofNullable(afiSafi);
    }

}
