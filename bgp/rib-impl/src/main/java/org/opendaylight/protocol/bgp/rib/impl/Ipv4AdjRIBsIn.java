/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import java.util.Comparator;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

final class Ipv4AdjRIBsIn extends AbstractAdjRIBsIn<Ipv4Prefix, Ipv4Route> {
	Ipv4AdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
		super(comparator, key);
	}

	@Override
	public InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<?> basePath, final Ipv4Prefix key) {
		final InstanceIdentifierBuilder<?> builder = InstanceIdentifier.builder(basePath);

		builder.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.Ipv4Routes.class);
		builder.node(Ipv4Route.class, new Ipv4RouteKey(key));

		return builder.toInstance();
	}

	@Override
	public void addRoutes(
			final DataModificationTransaction trans,
			final Peer peer,
			final MpReachNlri nlri,
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
		final RIBEntryData data = new RIBEntryData(attributes) {
			@Override
			protected Ipv4Route getDataObject(final Ipv4Prefix key) {
				return new Ipv4RouteBuilder().setKey(
						new Ipv4RouteKey(key)).setAttributes(
								new AttributesBuilder(attributes).build()).build();
			}
		};

		for (final Ipv4Prefix id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.destination.type.DestinationIpv4) nlri.getAdvertizedRoutes().getDestinationType()).getIpv4Prefixes()) {
			super.add(trans, peer, id, data);
		}
	}

	@Override
	public void removeRoutes(final DataModificationTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
		for (final Ipv4Prefix id : ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.destination.type.DestinationIpv4) nlri.getWithdrawnRoutes().getDestinationType()).getIpv4Prefixes()) {
			super.remove(trans, peer, id);
		}
	}
}