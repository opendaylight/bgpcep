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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.node._case.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.object.type.prefix._case.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;

final class LinkstateAdjRIBsIn extends AbstractAdjRIBsIn<CLinkstateDestination, LinkstateRoute> {
	private static abstract class LinkstateRIBEntryData<LSATTR extends LinkStateAttribute> extends
			RIBEntryData<CLinkstateDestination, LinkstateRoute> {
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
		return InstanceIdentifier.builder(basePath).child(LinkstateRoute.class,
				new LinkstateRouteKey(LinkstateNlriParser.serializeNlri(key))).toInstance();
	}

	@Override
	public void addRoutes(final DataModificationTransaction trans, final Peer peer, final MpReachNlri nlri,
			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes attributes) {
		final LinkstateDestination keys = ((LinkstateDestination) nlri.getAdvertizedRoutes().getDestinationType());

		for (final CLinkstateDestination key : keys.getCLinkstateDestination()) {
			final LinkStateAttribute lsattr = attributes.getAugmentation(PathAttributes1.class).getLinkstatePathAttribute().getLinkStateAttribute();

			RIBEntryData<CLinkstateDestination, LinkstateRoute> data = null;
			switch (key.getNlriType()) {
			case Ipv4Prefix:
			case Ipv6Prefix:
				data = new LinkstateRIBEntryData<PrefixAttributesCase>(attributes, (PrefixAttributesCase) lsattr) {
					@Override
					protected AttributeType createAttributes(final PrefixAttributesCase lsattr) {
						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.prefix._case.PrefixBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.prefix._case.PrefixBuilder();
						final PrefixAttributes a = lsattr.getPrefixAttributes();
						builder.setExtendedTags(a.getExtendedTags());
						builder.setIgpBits(a.getIgpBits());
						builder.setOspfForwardingAddress(a.getOspfForwardingAddress());
						builder.setPrefixMetric(a.getPrefixMetric());
						builder.setRouteTags(a.getRouteTags());
						return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.PrefixCaseBuilder().setPrefixAttributes(
								builder.build()).build();
					}

					@Override
					protected ObjectType createObject(final CLinkstateDestination key) {
						return new PrefixBuilder().setAdvertisingNodeDescriptors(
								new AdvertisingNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build()).build();
					}
				};
				break;
			case Link:
				data = new LinkstateRIBEntryData<LinkAttributesCase>(attributes, (LinkAttributesCase) lsattr) {
					@Override
					protected AttributeType createAttributes(final LinkAttributesCase lsattr) {
						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.link._case.LinkBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.link._case.LinkBuilder();
						final LinkAttributes la = lsattr.getLinkAttributes();
						builder.setAdminGroup(la.getAdminGroup());
						builder.setLinkName(la.getLinkName());
						builder.setLinkProtection(la.getLinkProtection());
						builder.setLocalIpv4RouterId(la.getLocalIpv4RouterId());
						builder.setLocalIpv6RouterId(la.getLocalIpv6RouterId());
						builder.setMaxLinkBandwidth(la.getMaxLinkBandwidth());
						builder.setMaxReservableBandwidth(la.getMaxReservableBandwidth());
						builder.setMetric(la.getMetric());
						builder.setMplsProtocol(la.getMplsProtocol());
						builder.setRemoteIpv4RouterId(la.getRemoteIpv4RouterId());
						builder.setRemoteIpv6RouterId(la.getRemoteIpv6RouterId());
						builder.setSharedRiskLinkGroups(la.getSharedRiskLinkGroups());
						builder.setTeMetric(la.getTeMetric());
						builder.setUnreservedBandwidth(la.getUnreservedBandwidth());
						return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.LinkCaseBuilder().setLink(
								builder.build()).build();
					}

					@Override
					protected ObjectType createObject(final CLinkstateDestination key) {
						final LinkCaseBuilder b = new LinkCaseBuilder();

						b.setLinkDescriptors(new LinkDescriptorsBuilder().setIpv4InterfaceAddress(value).getLinkDescriptors());
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
						final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.node._case.NodeBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.node._case.NodeBuilder();
						final NodeAttributes n = lsattr.getNodeAttributes();
						builder.setDynamicHostname(n.getDynamicHostname());
						builder.setIpv4RouterId(n.getIpv4RouterId());
						builder.setIpv6RouterId(n.getIpv6RouterId());
						builder.setIsisAreaId(n.getIsisAreaId());
						builder.setNodeFlags(n.getNodeFlags());
						builder.setTopologyIdentifier(n.getTopologyIdentifier());
						return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.loc.rib.tables.routes.linkstate.routes._case.linkstate.routes.linkstate.route.attributes.attribute.type.NodeCaseBuilder().setNode(
								builder.build()).build();
					}

					@Override
					protected ObjectType createObject(final CLinkstateDestination key) {
						final LocalNodeDescriptors d = key.getLocalNodeDescriptors();
						return new NodeCaseBuilder().setNode(
								new NodeBuilder().setAreaId(d.getAreaId()).setAsNumber(d.getAsNumber()).setCRouterIdentifier(
										d.getCRouterIdentifier()).setDomainId(d.getDomainId()).build()).build();
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