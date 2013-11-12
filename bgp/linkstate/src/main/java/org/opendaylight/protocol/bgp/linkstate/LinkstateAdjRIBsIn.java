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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.link.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.link.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.link.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.node.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

final class LinkstateAdjRIBsIn extends AbstractAdjRIBsIn<CLinkstateDestination, LinkstateRoute> {
	private static abstract class LinkstateRIBEntryData extends RIBEntryData<CLinkstateDestination, LinkstateRoute> {
		protected LinkstateRIBEntryData(final PathAttributes attributes) {
			super(attributes);
		}

		protected abstract ObjectType createObject(CLinkstateDestination key);

		@Override
		protected final LinkstateRoute getDataObject(final CLinkstateDestination key) {
			final LinkstateRouteBuilder builder = new LinkstateRouteBuilder();

			builder.setIdentifier(key.getIdentifier());
			builder.setProtocolId(key.getProtocolId());
			builder.setDistinguisher(key.getDistinguisher());
			builder.setAttributes(new AttributesBuilder(getPathAttributes()).build());
			builder.setObjectType(Preconditions.checkNotNull(createObject(key)));

			return builder.build();
		}
	}

	LinkstateAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
		super(comparator, key);
	}

	@Override
	public InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<Tables> basePath, final CLinkstateDestination key) {
		return InstanceIdentifier.builder(basePath).node(LinkstateRoute.class, new LinkstateRouteKey(LinkstateNlriParser.serializeNlri(key))).toInstance();
	}

	@Override
	public void addRoutes(
			final DataModificationTransaction trans,
			final Peer peer,
			final MpReachNlri nlri,
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
		final CLinkstateDestination key = (CLinkstateDestination) ((LinkstateDestination) nlri.getAdvertizedRoutes().getDestinationType()).getCLinkstateDestination();

		RIBEntryData<CLinkstateDestination, LinkstateRoute> data = null;
		switch (key.getNlriType()) {
		case Ipv4Prefix:
		case Ipv6Prefix:
			data = new LinkstateRIBEntryData(attributes) {
				@Override
				protected Prefix createObject(final CLinkstateDestination key) {
					return new PrefixBuilder(key.getPrefixDescriptors()).build();
				}
			};
			break;
		case Link:
			data = new LinkstateRIBEntryData(attributes) {
				@Override
				protected ObjectType createObject(final CLinkstateDestination key) {
					final LinkBuilder b = new LinkBuilder();

					b.setLinkDescriptors(new LinkDescriptorsBuilder(key.getLinkDescriptors()).build());
					b.setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build());
					b.setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder(key.getRemoteNodeDescriptors()).build());

					return b.build();
				}
			};
			break;
		case Node:
			data = new LinkstateRIBEntryData(attributes) {
				@Override
				protected ObjectType createObject(final CLinkstateDestination key) {
					return new NodeBuilder().setNodeDescriptors(new NodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build()).build();
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