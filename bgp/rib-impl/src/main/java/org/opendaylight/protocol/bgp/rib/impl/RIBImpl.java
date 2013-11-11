/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.concurrent.Future;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

@ThreadSafe
public class RIBImpl {
	private static final Logger logger = LoggerFactory.getLogger(RIBImpl.class);
	private final DataProviderService dps;
	private final RIBTables tables;

	public RIBImpl(final DataProviderService dps) {
		this.dps = Preconditions.checkNotNull(dps);
		this.tables = new RIBTables(BGPObjectComparator.INSTANCE, AdjRIBsInFactoryRegistryImpl.INSTANCE);
	}

	synchronized void updateTables(final BGPPeer peer, final Update message) {
		final DataModificationTransaction trans = this.dps.beginTransaction();

		// FIXME: detect and handle end-of-RIB markers

		// remove(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
		// trans, peer, message.getWithdrawnRoutes().getWithdrawnRoutes().iterator());

		final PathAttributes attrs = message.getPathAttributes();
		final PathAttributes2 mpu = attrs.getAugmentation(PathAttributes2.class);
		if (mpu != null) {
			final MpUnreachNlri nlri = mpu.getMpUnreachNlri();

			final AdjRIBsIn ari = this.tables.getOrCreate(new TablesKey(nlri.getAfi(), nlri.getSafi()));
			if (ari != null) {
				ari.removeRoutes(trans, peer, nlri);
			} else {
				logger.debug("Not removing objects from unhandled NLRI {}", nlri);
			}
		}

		// add(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
		// trans, peer, message.getNlri().getNlri().iterator(), attrs);

		final PathAttributes1 mpr = message.getPathAttributes().getAugmentation(PathAttributes1.class);
		if (mpr != null) {
			final MpReachNlri nlri = mpr.getMpReachNlri();

			final AdjRIBsIn ari = this.tables.getOrCreate(new TablesKey(nlri.getAfi(), nlri.getSafi()));
			if (ari != null) {
				ari.addRoutes(trans, peer, nlri, attrs);
			} else {
				logger.debug("Not adding objects from unhandled NLRI {}", nlri);
			}
		}

		// FIXME: we need to attach to this future for failures
		final Future<RpcResult<TransactionStatus>> f = trans.commit();
	}

	synchronized void clearTable(final BGPPeer peer, final TablesKey key) {
		final AdjRIBsIn ari = this.tables.get(key);
		if (ari != null) {
			final DataModificationTransaction trans = this.dps.beginTransaction();
			ari.clear(trans, peer);

			// FIXME: we need to attach to this future for failures
			final Future<RpcResult<TransactionStatus>> f = trans.commit();
		}
	}

	@Override
	public String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper;
	}
}
