/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.Comparator;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBsIn;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;

final class LinkstateAdjRIBsIn extends AbstractAdjRIBsIn<CLinkstateDestination, DataObject> {
	LinkstateAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
		super(comparator, key);
	}

	@Override
	public InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<?> basePath, final CLinkstateDestination key) {
		final InstanceIdentifierBuilder<?> builder = InstanceIdentifier.builder(basePath);

		switch (key.getNlriType()) {
		case Ipv4Prefix:
			// FIXME: finish this
			return builder.toInstance();
		case Ipv6Prefix:
			// FIXME: finish this
			return builder.toInstance();
		case Link:
			// FIXME: finish this
			return builder.toInstance();
		case Node:
			// FIXME: finish this
			return builder.toInstance();
		}

		throw new IllegalStateException("Unhandled link-state NLRI type " + key.getNlriType());
	}

	@Override
	public void addRoutes(
			final DataModificationTransaction trans,
			final Peer peer,
			final MpReachNlri nlri,
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
		final CLinkstateDestination key = (CLinkstateDestination) ((LinkstateDestination) nlri.getAdvertizedRoutes().getDestinationType()).getCLinkstateDestination();

		RIBEntryData data = null;
		switch (key.getNlriType()) {
		case Ipv4Prefix:
			data = new RIBEntryData(attributes) {
				@Override
				protected DataObject getDataObject(final CLinkstateDestination key) {
					// TODO Auto-generated method stub
					return null;
				}
			};
			break;
		case Ipv6Prefix:
			data = new RIBEntryData(attributes) {
				@Override
				protected DataObject getDataObject(final CLinkstateDestination key) {
					// TODO Auto-generated method stub
					return null;
				}
			};
			break;
		case Link:
			data = new RIBEntryData(attributes) {
				@Override
				protected DataObject getDataObject(final CLinkstateDestination key) {
					// TODO Auto-generated method stub
					return null;
				}
			};
			break;
		case Node:
			data = new RIBEntryData(attributes) {
				@Override
				protected DataObject getDataObject(final CLinkstateDestination key) {
					// TODO Auto-generated method stub
					return null;
				}
			};
			break;
		}

		if (data == null) {
			throw new IllegalStateException("Unhandled link-state NLRI type " + key.getNlriType());
		}
		super.add(trans, peer, key, data);
	}

	@Override
	public void removeRoutes(final DataModificationTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
		final CLinkstateDestination key = (CLinkstateDestination) ((LinkstateDestination) nlri.getWithdrawnRoutes().getDestinationType()).getCLinkstateDestination();

		super.remove(trans, peer, key);
	}
}