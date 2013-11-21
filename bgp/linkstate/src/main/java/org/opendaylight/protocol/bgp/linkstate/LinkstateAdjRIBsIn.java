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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.link.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.node.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.prefix.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.link.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.link.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.link.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.node.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.object.type.prefix.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

final class LinkstateAdjRIBsIn extends AbstractAdjRIBsIn<CLinkstateDestination, LinkstateRoute> {
	private static abstract class LinkstateRIBEntryData<LSATTR extends LinkStateAttribute> extends RIBEntryData<CLinkstateDestination, LinkstateRoute> {
		private final LSATTR lsattr;

		protected LinkstateRIBEntryData(final PathAttributes attributes, final LSATTR lsattr) {
			super(attributes);
			this.lsattr = Preconditions.checkNotNull(lsattr);
		}

		protected abstract AttributeType createAttributes(LSATTR lsattr);
		protected abstract ObjectType createObject(CLinkstateDestination key);

		@Override
		protected final LinkstateRoute getDataObject(final CLinkstateDestination key) {
			final LinkstateRouteBuilder builder = new LinkstateRouteBuilder();

			builder.setIdentifier(key.getIdentifier());
			builder.setProtocolId(key.getProtocolId());
			builder.setDistinguisher(key.getDistinguisher());
			builder.setAttributes(new AttributesBuilder(getPathAttributes()).
					addAugmentation(Attributes1.class, new Attributes1Builder().setAttributeType(
							Preconditions.checkNotNull(createAttributes(lsattr))).build()).
							build());
			builder.setObjectType(Preconditions.checkNotNull(createObject(key)));

			return builder.build();
		}
	}

	LinkstateAdjRIBsIn(final Comparator<PathAttributes> comparator, final TablesKey key) {
		super(comparator, key);
	}

	@Override
	public InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<Tables> basePath, final CLinkstateDestination key) {
		return InstanceIdentifier.builder(basePath).child(LinkstateRoute.class, new LinkstateRouteKey(LinkstateNlriParser.serializeNlri(key))).toInstance();
	}

	@Override
	public void addRoutes(
			final DataModificationTransaction trans,
			final Peer peer,
			final MpReachNlri nlri,
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes attributes) {
		final CLinkstateDestination key = (CLinkstateDestination) ((LinkstateDestination) nlri.getAdvertizedRoutes().getDestinationType()).getCLinkstateDestination();

		final LinkStateAttribute lsattr = attributes.getAugmentation(PathAttributes1.class).
				getLinkstatePathAttribute().getLinkStateAttribute();

		RIBEntryData<CLinkstateDestination, LinkstateRoute> data = null;
		switch (key.getNlriType()) {
		case Ipv4Prefix:
		case Ipv6Prefix:
			data = new LinkstateRIBEntryData<PrefixAttributes>(attributes, (PrefixAttributes)lsattr) {
				@Override
				protected AttributeType createAttributes(final PrefixAttributes lsattr) {
					return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.PrefixBuilder().
							setPrefixAttributes(new PrefixAttributesBuilder(lsattr).build()).build();
				}

				@Override
				protected Prefix createObject(final CLinkstateDestination key) {
					return new PrefixBuilder(key.getPrefixDescriptors()).
							setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build()).
							build();
				}
			};
			break;
		case Link:
			data = new LinkstateRIBEntryData<LinkAttributes>(attributes, (LinkAttributes)lsattr) {
				@Override
				protected AttributeType createAttributes(final LinkAttributes lsattr) {
					return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.LinkBuilder().
							setLinkAttributes(new LinkAttributesBuilder(lsattr).build()).build();
				}

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
			data = new LinkstateRIBEntryData<NodeAttributes>(attributes, (NodeAttributes)lsattr) {
				@Override
				protected AttributeType createAttributes(final NodeAttributes lsattr) {
					return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.loc.rib.tables.routes.linkstate.routes.linkstate.route.attributes.attribute.type.NodeBuilder().
							setNodeAttributes(new NodeAttributesBuilder(lsattr).build()).build();
				}

				@Override
				protected ObjectType createObject(final CLinkstateDestination key) {
					return new NodeBuilder().setNodeDescriptors(new NodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build()).build();
				}
			};
			break;
		}

		super.add(trans, peer, key, data);
	}

	@Override
	public void removeRoutes(final DataModificationTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
		final CLinkstateDestination key = (CLinkstateDestination) ((LinkstateDestination) nlri.getWithdrawnRoutes().getDestinationType()).getCLinkstateDestination();

		super.remove(trans, peer, key);
	}
}