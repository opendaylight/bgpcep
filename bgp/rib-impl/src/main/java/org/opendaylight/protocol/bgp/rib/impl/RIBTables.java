/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsFactory;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
final class RIBTables {

    private static final Logger LOG = LoggerFactory.getLogger(RIBTables.class);

    private final Map<TablesKey, AdjRIBsIn<?, ?>> tables = new HashMap<>();
    private final RIBExtensionConsumerContext registry;

    RIBTables(final RIBExtensionConsumerContext extensions) {
        this.registry = Preconditions.checkNotNull(extensions);
    }

    public synchronized AdjRIBsIn<?, ?> get(final TablesKey key) {
        LOG.debug("Looking for key {} in tables {}", key, this.tables);
        final AdjRIBsIn<?, ?> ret = this.tables.get(key);
        LOG.trace("Key found {}", ret);
        return ret;
    }

    public synchronized AdjRIBsIn<?, ?> create(final WriteTransaction trans, final RibReference rib, final TablesKey key) {
        if (this.tables.containsKey(key)) {
            LOG.warn("Duplicate create request for key {}", key);
            return this.tables.get(key);
        }

        final AdjRIBsFactory f = this.registry.getAdjRIBsInFactory(key.getAfi(), key.getSafi());
        if (f == null) {
            LOG.debug("RIBsInFactory not found for key {}, returning null", key);
            return null;
        }

        final KeyedInstanceIdentifier<Tables, TablesKey> basePath = rib.getInstanceIdentifier().child(LocRib.class).child(Tables.class, key);
        final AdjRIBsIn<?, ?> table = Preconditions.checkNotNull(f.createAdjRIBs(basePath));
        LOG.debug("Table {} created for key {}", table, key);
        this.tables.put(key, table);

        trans.put(LogicalDatastoreType.OPERATIONAL, basePath,
                new TablesBuilder().setAfi(key.getAfi()).setSafi(key.getSafi())
                .setAttributes(new AttributesBuilder().setUptodate(Boolean.TRUE).build()).build());

        return table;
    }
}
