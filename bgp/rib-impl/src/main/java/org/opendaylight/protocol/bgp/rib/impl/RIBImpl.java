/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataModification.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.spi.NLRIHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

@ThreadSafe
public final class RIBImpl {
	private static final Logger logger = LoggerFactory.getLogger(RIBImpl.class);

	@GuardedBy("this")
	private final Map<TablesKey, RIBTable<Object>> adjRIBsIn = new HashMap<>();

	private final DataProviderService dps;
	private final String name;

	public RIBImpl(final String name, final ProviderContext context) {
		this.name = Preconditions.checkNotNull(name);
		this.dps = context.getSALService(DataProviderService.class);
	}

	private RIBTable<Object> getTable(final TablesKey key) {
		final RIBTable<Object> table;
		if (!adjRIBsIn.containsKey(key)) {
			table = new RIBTable<Object>(LocRib.class, key);
			adjRIBsIn.put(key, table);
		} else {
			table = adjRIBsIn.get(key);
		}

		return table;
	}

	private void add(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
			final DataModification trans, final BGPPeer peer, final Iterator<?> it, final PathAttributes attrs) {
		final TablesKey key = new TablesKey(afi, safi);
		final RIBTable<Object> table = getTable(key);

		while (it.hasNext()) {
			table.add(trans, peer, it.next(), attrs);
		}
	}

	private void remove(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
			final DataModification trans, final BGPPeer peer, final Iterator<?> it) {
		final TablesKey key = new TablesKey(afi, safi);
		final RIBTable<Object> table = getTable(key);

		while (it.hasNext()) {
			table.remove(trans, peer, it.next());
		}
	}

	synchronized void updateTables(final BGPPeer peer, final Update message) {
		final DataModification trans = dps.beginTransaction();

		remove(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
				trans, peer, message.getWithdrawnRoutes().getWithdrawnRoutes().iterator());

		final PathAttributes attrs = message.getPathAttributes();
		final PathAttributes2 mpu = attrs.getAugmentation(PathAttributes2.class);
		if (mpu != null) {
			final MpUnreachNlri nlri = mpu.getMpUnreachNlri();

			final NLRIHandler<?> handler = NLRIHandlerRegistryImpl.INSTANCE.getHandler(nlri.getAfi(), nlri.getSafi());
			if (handler != null) {
				remove(nlri.getAfi(), nlri.getSafi(), trans, peer, handler.getKeys(nlri));
			} else {
				logger.debug("Not removing objects from unhandled NLRI {}", nlri);
			}
		}

		add(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
				trans, peer, message.getNlri().getNlri().iterator(), attrs);

		final PathAttributes1 mpr = message.getPathAttributes().getAugmentation(PathAttributes1.class);
		if (mpr != null) {
			final MpReachNlri nlri = mpr.getMpReachNlri();

			final NLRIHandler<?> handler = NLRIHandlerRegistryImpl.INSTANCE.getHandler(nlri.getAfi(), nlri.getSafi());
			if (handler != null) {
				add(nlri.getAfi(), nlri.getSafi(), trans, peer, handler.getKeys(nlri), attrs);
			} else {
				logger.debug("Not adding objects from unhandled NLRI {}", nlri);
			}
		}

		// FIXME: we need to attach to this future for failures
		Future<RpcResult<TransactionStatus>> f = trans.commit();
	}

	synchronized void clearTable(final BGPPeer peer, final TablesKey key) {
		final RIBTable<Object> t = adjRIBsIn.get(key);
		if (t != null) {
			final DataModification trans = dps.beginTransaction();
			t.clear(trans, peer);

			// FIXME: we need to attach to this future for failures
			Future<RpcResult<TransactionStatus>> f = trans.commit();
		}
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		toStringHelper.add("name", this.name);
		return toStringHelper;
	}
}
