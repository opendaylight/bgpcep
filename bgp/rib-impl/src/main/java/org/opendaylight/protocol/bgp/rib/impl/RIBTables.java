package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsInFactory;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

final class RIBTables {

	private static final Logger LOG = LoggerFactory.getLogger(RIBTables.class);

	private final Map<TablesKey, AdjRIBsIn> tables = new HashMap<>();
	private final Comparator<PathAttributes> comparator;
	private final RIBExtensionConsumerContext registry;

	RIBTables(final Comparator<PathAttributes> comparator, final RIBExtensionConsumerContext extensions) {
		this.comparator = Preconditions.checkNotNull(comparator);
		this.registry = Preconditions.checkNotNull(extensions);
	}

	public synchronized AdjRIBsIn get(final TablesKey key) {
		return this.tables.get(key);
	}

	public synchronized AdjRIBsIn getOrCreate(final DataModificationTransaction trans, final RibReference rib, final TablesKey key) {
		LOG.debug("Looking for key {} in tables {}", key, this.tables);
		if (this.tables.containsKey(key)) {
			LOG.trace("Key found {}.", this.tables.get(key));
			return this.tables.get(key);
		}

		final AdjRIBsInFactory f = this.registry.getAdjRIBsInFactory(key.getAfi(), key.getSafi());
		if (f == null) {
			LOG.debug("RIBsInFactory not found for key {}, returning null", key);
			return null;
		}

		final AdjRIBsIn table = Preconditions.checkNotNull(f.createAdjRIBsIn(trans, rib, this.comparator, key));
		LOG.debug("Table {} created for key {}", table, key);
		this.tables.put(key, table);
		return table;
	}
}
