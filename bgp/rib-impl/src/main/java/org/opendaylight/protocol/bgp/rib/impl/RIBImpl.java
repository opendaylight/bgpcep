/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;

@ThreadSafe
public class RIBImpl extends DefaultRibReference {
	private static final Logger LOG = LoggerFactory.getLogger(RIBImpl.class);
	private static final Update EOR = new UpdateBuilder().build();
	private final DataProviderService dps;
	private final RIBTables tables;

	public RIBImpl(final RibId ribId, final RIBExtensionConsumerContext extensions, final DataProviderService dps) {
		super(InstanceIdentifier.builder(BgpRib.class).child(Rib.class, new RibKey(ribId)).toInstance());
		this.dps = Preconditions.checkNotNull(dps);
		this.tables = new RIBTables(BGPObjectComparator.INSTANCE, extensions);
	}

	synchronized void updateTables(final BGPPeer peer, final Update message) {
		final DataModificationTransaction trans = this.dps.beginTransaction();

		if (EOR.equals(message)) {
			final AdjRIBsIn ari = this.tables.getOrCreate(trans, this,
					new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
			if (ari != null) {
				ari.markUptodate(trans, peer);
			} else {
				LOG.debug("End-of-RIB for IPv4 Unicast ignored");
			}
			return;
		}

		final WithdrawnRoutes wr = message.getWithdrawnRoutes();
		if (wr != null) {
			final AdjRIBsIn ari = this.tables.getOrCreate(trans, this,
					new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
			if (ari != null) {
				ari.removeRoutes(
						trans,
						peer,
						new MpUnreachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setWithdrawnRoutes(
								new WithdrawnRoutesBuilder().setDestinationType(
										new DestinationIpv4CaseBuilder().setDestinationIpv4(
												new DestinationIpv4Builder().setIpv4Prefixes(wr.getWithdrawnRoutes()).build()).build()).build()).build());
			} else {
				LOG.debug("Not removing objects from unhandled IPv4 Unicast");
			}
		}

		final PathAttributes attrs = message.getPathAttributes();
		final PathAttributes2 mpu = attrs.getAugmentation(PathAttributes2.class);
		if (mpu != null) {
			final MpUnreachNlri nlri = mpu.getMpUnreachNlri();

			final AdjRIBsIn ari = this.tables.getOrCreate(trans, this, new TablesKey(nlri.getAfi(), nlri.getSafi()));
			if (ari != null) {
				ari.removeRoutes(trans, peer, nlri);
			} else {
				LOG.debug("Not removing objects from unhandled NLRI {}", nlri);
			}
		}

		final Nlri ar = message.getNlri();
		if (ar != null) {
			final AdjRIBsIn ari = this.tables.getOrCreate(trans, this,
					new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
			if (ari != null) {
				ari.addRoutes(
						trans,
						peer,
						new MpReachNlriBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setCNextHop(
								attrs.getCNextHop()).setAdvertizedRoutes(
										new AdvertizedRoutesBuilder().setDestinationType(
												new DestinationIpv4CaseBuilder().setDestinationIpv4(
														new DestinationIpv4Builder().setIpv4Prefixes(ar.getNlri()).build()).build()).build()).build(),
														attrs);
			} else {
				LOG.debug("Not adding objects from unhandled IPv4 Unicast");
			}
		}

		final PathAttributes1 mpr = message.getPathAttributes().getAugmentation(PathAttributes1.class);
		if (mpr != null) {
			final MpReachNlri nlri = mpr.getMpReachNlri();

			final AdjRIBsIn ari = this.tables.getOrCreate(trans, this, new TablesKey(nlri.getAfi(), nlri.getSafi()));
			if (ari != null) {
				ari.addRoutes(trans, peer, nlri, attrs);
				if (message.equals(ari.endOfRib())) {
					ari.markUptodate(trans, peer);
				}
			} else {
				LOG.debug("Not adding objects from unhandled NLRI {}", nlri);
			}
		}

		Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
			public void onSuccess(final RpcResult<TransactionStatus> result) {
				LOG.debug("RIB modification successfully committed.");
			}

			@Override
			public void onFailure(final Throwable t) {
				LOG.error("Failed to commit RIB modification", t);
			}
		});
	}

	synchronized void clearTable(final BGPPeer peer, final TablesKey key) {
		final AdjRIBsIn ari = this.tables.get(key);
		if (ari != null) {
			final DataModificationTransaction trans = this.dps.beginTransaction();
			ari.clear(trans, peer);

			Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.commit()), new FutureCallback<RpcResult<TransactionStatus>>() {
				@Override
				public void onSuccess(final RpcResult<TransactionStatus> result) {
					// Nothing to do
				}

				@Override
				public void onFailure(final Throwable t) {
					LOG.error("Failed to commit RIB modification", t);
				}
			});
		}
	}

	@Override
	public final String toString() {
		return addToStringAttributes(Objects.toStringHelper(this)).toString();
	}

	protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
		return toStringHelper;
	}
}
