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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

final class LinkstateAdjRIBsIn extends AbstractAdjRIBsIn<CLinkstateDestination, LinkstateRoute> {
	private abstract static class LinkstateRIBEntryData<A extends LinkStateAttribute> extends
			RIBEntryData<CLinkstateDestination, LinkstateRoute> {
		private final A lsattr;

		protected LinkstateRIBEntryData(final PathAttributes attributes, final A lsattr) {
			super(attributes);
			this.lsattr = Preconditions.checkNotNull(lsattr);
		}

		protected abstract AttributeType createAttributes(A lsattr);

		protected abstract ObjectType createObject(CLinkstateDestination key);

		@Override
		protected final LinkstateRoute getDataObject(final CLinkstateDestination key) {
			final LinkstateRouteBuilder builder = new LinkstateRouteBuilder();

			builder.setIdentifier(key.getIdentifier());
			builder.setProtocolId(key.getProtocolId());
			builder.setDistinguisher(key.getDistinguisher());
			builder.setAttributes(new AttributesBuilder(getPathAttributes()).addAugmentation(Attributes1.class,
					new Attributes1Builder().setAttributeType(Preconditions.checkNotNull(createAttributes(this.lsattr))).build()).build());
			builder.setObjectType(Preconditions.checkNotNull(createObject(key)));

			return builder.build();
		}
	}

	LinkstateAdjRIBsIn(final DataModificationTransaction trans, final Comparator<PathAttributes> comparator, final TablesKey key) {
		super(trans, comparator, key);
	}

	@Override
	public InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<Tables> basePath, final CLinkstateDestination key) {
		return InstanceIdentifier.builder(basePath).child(LinkstateRoutes.class).child(LinkstateRoute.class,
				new LinkstateRouteKey(LinkstateNlriParser.serializeNlri(key))).toInstance();
	}

	@Override
	public void addRoutes(final DataModificationTransaction trans, final Peer peer, final MpReachNlri nlri,
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes attributes) {
		final LinkstateDestination keys = ((DestinationLinkstateCase) nlri.getAdvertizedRoutes().getDestinationType()).getDestinationLinkstate();

		for (final CLinkstateDestination key : keys.getCLinkstateDestination()) {
			final LinkStateAttribute lsattr = attributes.getAugmentation(PathAttributes1.class).getLinkstatePathAttribute().getLinkStateAttribute();

			RIBEntryData<CLinkstateDestination, LinkstateRoute> data = null;
			switch (key.getNlriType()) {
			case Ipv4Prefix:
			case Ipv6Prefix:
				data = new LinkstateRIBEntryData<PrefixAttributesCase>(attributes, (PrefixAttributesCase) lsattr) {
					@Override
					protected AttributeType createAttributes(final PrefixAttributesCase lsattr) {
						final PrefixAttributes a = lsattr.getPrefixAttributes();
						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.prefix._case.PrefixAttributesBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.prefix._case.PrefixAttributesBuilder(a);
						return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.PrefixCaseBuilder().setPrefixAttributes(
								builder.build()).build();
					}

					@Override
					protected ObjectType createObject(final CLinkstateDestination key) {
						return new PrefixCaseBuilder().setAdvertisingNodeDescriptors(
								new AdvertisingNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build()).build();
					}
				};
				break;
			case Link:
				data = new LinkstateRIBEntryData<LinkAttributesCase>(attributes, (LinkAttributesCase) lsattr) {
					@Override
					protected AttributeType createAttributes(final LinkAttributesCase lsattr) {
						final LinkAttributes la = lsattr.getLinkAttributes();
						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.link._case.LinkAttributesBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.link._case.LinkAttributesBuilder(la);
						return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.LinkCaseBuilder().setLinkAttributes(
								builder.build()).build();
					}

					@Override
					protected ObjectType createObject(final CLinkstateDestination key) {
						final LinkCaseBuilder b = new LinkCaseBuilder();
						b.setLinkDescriptors(new LinkDescriptorsBuilder(key.getLinkDescriptors()).build());
						b.setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build());
						b.setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder(key.getRemoteNodeDescriptors()).build());
						return b.build();
					}
				};
				break;
			case Node:
				data = new LinkstateRIBEntryData<NodeAttributesCase>(attributes, (NodeAttributesCase) lsattr) {
					@Override
					protected AttributeType createAttributes(final NodeAttributesCase lsattr) {
						final NodeAttributes n = lsattr.getNodeAttributes();
						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.node._case.NodeAttributesBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.node._case.NodeAttributesBuilder(n);
						return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.NodeCaseBuilder().setNodeAttributes(
								builder.build()).build();
					}

					@Override
					protected ObjectType createObject(final CLinkstateDestination key) {
						final LocalNodeDescriptors d = key.getLocalNodeDescriptors();
						return new NodeCaseBuilder().setNodeDescriptors(new NodeDescriptorsBuilder(d).build()).build();
					}
				};
				break;
			}
			super.add(trans, peer, key, data);
		}
	}

	@Override
	public void removeRoutes(final DataModificationTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
		final CLinkstateDestination key = (CLinkstateDestination) ((LinkstateDestination) nlri.getWithdrawnRoutes().getDestinationType()).getCLinkstateDestination();

		super.remove(trans, peer, key);
	}
}