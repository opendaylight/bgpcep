package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;

import com.google.common.base.Preconditions;

final class RIBTables {
	private final Map<TablesKey, AdjRIBsIn> tables = new HashMap<>();
	private final Comparator<PathAttributes> comparator;
	private final RIBExtensionConsumerContext registry;

	RIBTables(final Comparator<PathAttributes> comparator, final RIBExtensionConsumerContext extensions) {
		this.comparator = Preconditions.checkNotNull(comparator);
		this.registry = Preconditions.checkNotNull(extensions);
	}

	public synchronized AdjRIBsIn get(final TablesKey key) {
		return tables.get(key);
	}

	public synchronized AdjRIBsIn getOrCreate(final TablesKey key) {
		final AdjRIBsIn table;

		if (!tables.containsKey(key)) {
			table = registry.getAdjRIBsInFactory(key.getAfi(), key.getSafi()).createAdjRIBsIn(comparator, key);
			tables.put(key, table);
		} else {
			table = tables.get(key);
		}

		return table;
	}

}
