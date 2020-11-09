/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.openconfig.spi;

import com.google.common.collect.BiMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;

abstract class AbstractBGPTableTypeRegistryConsumer implements BGPTableTypeRegistryConsumer {
    @Override
    public final BgpTableType getTableType(final Class<? extends AfiSafiType> afiSafiType) {
        return tableTypes().inverse().get(afiSafiType);
    }

    @Override
    public final TablesKey getTableKey(final Class<? extends AfiSafiType> afiSafiType) {
        return tableKeys().inverse().get(afiSafiType);
    }

    @Override
    public final Class<? extends AfiSafiType> getAfiSafiType(final BgpTableType bgpTableType) {
        return tableTypes().get(bgpTableType);
    }

    @Override
    public final Class<? extends AfiSafiType> getAfiSafiType(final TablesKey tablesKey) {
        return tableKeys().get(tablesKey);
    }

    abstract @NonNull BiMap<BgpTableType, Class<? extends AfiSafiType>> tableTypes();

    abstract @NonNull BiMap<TablesKey, Class<? extends AfiSafiType>> tableKeys();
}
