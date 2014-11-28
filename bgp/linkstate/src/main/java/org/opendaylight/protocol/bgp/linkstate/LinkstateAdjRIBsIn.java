/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.rib.spi.AbstractAdjRIBs;
import org.opendaylight.protocol.bgp.rib.spi.AdjRIBsTransaction;
import org.opendaylight.protocol.bgp.rib.spi.Peer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.CLinkstateDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.LinkstateRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.LinkstateRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.AttributeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.link._case.LinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.node._case.NodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.prefix._case.PrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.LinkstatePathAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLinkstateCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.AttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LinkstateAdjRIBsIn extends AbstractAdjRIBs<CLinkstateDestination, LinkstateRoute, LinkstateRouteKey> {

    private abstract static class LinkstateRIBEntryData<A extends LinkStateAttribute> extends RIBEntryData<CLinkstateDestination, LinkstateRoute, LinkstateRouteKey> {
        private final A lsattr;

        protected LinkstateRIBEntryData(final Peer peer, final PathAttributes attributes, final A lsattr) {
            super(peer, attributes);
            this.lsattr = lsattr;
        }

        protected abstract AttributeType createAttributes(A lsattr);

        protected abstract ObjectType createObject(CLinkstateDestination key);

        @Override
        protected final LinkstateRoute getDataObject(final CLinkstateDestination key, final LinkstateRouteKey id) {
            final LinkstateRouteBuilder builder = new LinkstateRouteBuilder();

            builder.setKey(id);
            builder.setIdentifier(key.getIdentifier());
            builder.setProtocolId(key.getProtocolId());
            builder.setDistinguisher(key.getDistinguisher());
            builder.setAttributes(new AttributesBuilder(getPathAttributes()).addAugmentation(Attributes1.class,
                new Attributes1Builder().setAttributeType(Preconditions.checkNotNull(createAttributes(this.lsattr))).build()).build());
            builder.setObjectType(Preconditions.checkNotNull(createObject(key)));

            return builder.build();
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("lsattr", this.lsattr);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateAdjRIBsIn.class);
    private final InstanceIdentifier<LinkstateRoutes> routesBasePath;

    LinkstateAdjRIBsIn(final KeyedInstanceIdentifier<Tables, TablesKey> basePath) {
        super(basePath);
        this.routesBasePath = basePath.builder().child((Class)LinkstateRoutes.class).build();
    }

    @Override
    @Deprecated
    public KeyedInstanceIdentifier<LinkstateRoute, LinkstateRouteKey> identifierForKey(final InstanceIdentifier<Tables> basePath, final CLinkstateDestination key) {
        final ByteBuf keyBuf = Unpooled.buffer();
        LinkstateNlriParser.serializeNlri(key, keyBuf);
        return basePath.child((Class)LinkstateRoutes.class).child(LinkstateRoute.class,
            new LinkstateRouteKey(ByteArray.readAllBytes(keyBuf)));
    }

    @Override
    public KeyedInstanceIdentifier<LinkstateRoute, LinkstateRouteKey> identifierForKey(final CLinkstateDestination key) {
        final ByteBuf keyBuf = Unpooled.buffer();
        LinkstateNlriParser.serializeNlri(key, keyBuf);
        return this.routesBasePath.child(LinkstateRoute.class,
            new LinkstateRouteKey(ByteArray.readAllBytes(keyBuf)));
    }

    @Override
    public void addRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpReachNlri nlri,
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes attributes) {
        LOG.debug("Passed nlri {}", nlri);
        final LinkstateDestination keys = ((DestinationLinkstateCase) nlri.getAdvertizedRoutes().getDestinationType()).getDestinationLinkstate();
        if (keys == null) {
            LOG.debug("No destinations present in advertized routes");
            return;
        }

        LOG.debug("Iterating over route destinations {}", keys);

        for (final CLinkstateDestination key : keys.getCLinkstateDestination()) {
            LOG.debug("Processing route key {}", key);

            LinkStateAttribute lsattr = null;
            final PathAttributes1 pa = attributes.getAugmentation(PathAttributes1.class);
            if (pa != null) {
                final LinkstatePathAttribute lpa = pa.getLinkstatePathAttribute();
                if (lpa != null) {
                    lsattr = lpa.getLinkStateAttribute();
                }
            }

            RIBEntryData<CLinkstateDestination, LinkstateRoute, LinkstateRouteKey> data = null;
            switch (key.getNlriType()) {
            case Ipv4Prefix:
            case Ipv6Prefix:
                data = new LinkstateRIBEntryData<PrefixAttributesCase>(peer, attributes, (PrefixAttributesCase) lsattr) {
                    @Override
                    protected AttributeType createAttributes(final PrefixAttributesCase lsattr) {
                        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.PrefixCaseBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.PrefixCaseBuilder();
                        if (lsattr != null && lsattr.getPrefixAttributes() != null) {
                            b.setPrefixAttributes(new PrefixAttributesBuilder(lsattr.getPrefixAttributes()).build());
                        }
                        return b.build();
                    }

                    @Override
                    protected PrefixCase createObject(final CLinkstateDestination key) {
                        final PrefixCaseBuilder b = new PrefixCaseBuilder(key.getPrefixDescriptors());
                        b.setAdvertisingNodeDescriptors(new AdvertisingNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build());
                        return b.build();
                    }
                };
                break;
            case Link:
                data = new LinkstateRIBEntryData<LinkAttributesCase>(peer, attributes, (LinkAttributesCase) lsattr) {
                    @Override
                    protected AttributeType createAttributes(final LinkAttributesCase lsattr) {
                        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.LinkCaseBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.LinkCaseBuilder();
                        if (lsattr != null && lsattr.getLinkAttributes() != null) {
                            b.setLinkAttributes(new LinkAttributesBuilder(lsattr.getLinkAttributes()).build());
                        }
                        return b.build();
                    }

                    @Override
                    protected LinkCase createObject(final CLinkstateDestination key) {
                        final LinkCaseBuilder b = new LinkCaseBuilder();
                        b.setLinkDescriptors(new LinkDescriptorsBuilder(key.getLinkDescriptors()).build());
                        b.setLocalNodeDescriptors(new LocalNodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build());
                        b.setRemoteNodeDescriptors(new RemoteNodeDescriptorsBuilder(key.getRemoteNodeDescriptors()).build());
                        return b.build();
                    }
                };
                break;
            case Node:
                data = new LinkstateRIBEntryData<NodeAttributesCase>(peer, attributes, (NodeAttributesCase) lsattr) {
                    @Override
                    protected AttributeType createAttributes(final NodeAttributesCase lsattr) {
                        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.NodeCaseBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.routes.linkstate.routes.linkstate.route.attributes.attribute.type.NodeCaseBuilder();
                        if (lsattr != null && lsattr.getNodeAttributes() != null) {
                            b.setNodeAttributes(new NodeAttributesBuilder(lsattr.getNodeAttributes()).build());
                        }
                        return b.build();
                    }

                    @Override
                    protected NodeCase createObject(final CLinkstateDestination key) {
                        return new NodeCaseBuilder().setNodeDescriptors(new NodeDescriptorsBuilder(key.getLocalNodeDescriptors()).build()).build();
                    }
                };
                break;
            default:
                break;
            }
            super.add(trans, peer, key, data);
        }
    }

    @Override
    public void removeRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpUnreachNlri nlri) {
        final DestinationLinkstate keys = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) nlri.getWithdrawnRoutes().getDestinationType()).getDestinationLinkstate();

        for (final CLinkstateDestination key : keys.getCLinkstateDestination()) {
            super.remove(trans, peer, key);
        }
    }

    @Override
    public void addAdvertisement(final MpReachNlriBuilder builder, final LinkstateRoute data) {
        final CLinkstateDestinationBuilder nlri = new CLinkstateDestinationBuilder();
        nlri.setProtocolId(data.getProtocolId());
        nlri.setIdentifier(data.getIdentifier());
        final Attributes a = data.getAttributes();
        if (a != null && a.getCNextHop() != null) {
            builder.setCNextHop(a.getCNextHop());
        }
        final ObjectType type = data.getObjectType();
        if (type instanceof PrefixCase) {
            final PrefixCase prefix = (PrefixCase) type;
            final IpPrefix ip = prefix.getIpReachabilityInformation();

            if (ip.getIpv4Prefix() != null) {
                nlri.setNlriType(NlriType.Ipv4Prefix);
            } else if (ip.getIpv6Prefix() != null) {
                nlri.setNlriType(NlriType.Ipv6Prefix);
            } else {
                throw new IllegalArgumentException("Unsupported reachability type " + ip);
            }
            nlri.setLocalNodeDescriptors(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder(prefix.getAdvertisingNodeDescriptors()).build());

        } else if (type instanceof NodeCase) {
            final NodeCase node = (NodeCase) type;

            nlri.setNlriType(NlriType.Node);
            nlri.setLocalNodeDescriptors(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder(node.getNodeDescriptors()).build());
        } else if (type instanceof LinkCase) {
            final LinkCase link = (LinkCase) type;

            nlri.setNlriType(NlriType.Link);
            nlri.setLinkDescriptors(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LinkDescriptorsBuilder(link.getLinkDescriptors()).build());
            nlri.setLocalNodeDescriptors(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder(link.getLocalNodeDescriptors()).build());
            nlri.setRemoteNodeDescriptors(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptorsBuilder(link.getRemoteNodeDescriptors()).build());
        } else {
            throw new IllegalArgumentException("Unhandled linkstate route type " + type);
        }

        final AdvertizedRoutes ar = builder.getAdvertizedRoutes();
        if (ar == null) {
            builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
                new DestinationLinkstateCaseBuilder().setDestinationLinkstate(new DestinationLinkstateBuilder()
                    .setCLinkstateDestination(Lists.newArrayList(nlri.build())).build()).build()).build());
        } else {
            ((DestinationLinkstateCase) ar.getDestinationType()).getDestinationLinkstate().getCLinkstateDestination().add(nlri.build());
        }
    }

    @Override
    public void addWithdrawal(final MpUnreachNlriBuilder builder, final CLinkstateDestination id) {
        final WithdrawnRoutes wr = builder.getWithdrawnRoutes();
        if (wr == null) {
            builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCaseBuilder().setDestinationLinkstate(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.linkstate._case.DestinationLinkstateBuilder()
                        .setCLinkstateDestination(Lists.newArrayList(id)).build()).build()).build());
        } else {
            ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationLinkstateCase) wr.getDestinationType())
            .getDestinationLinkstate().getCLinkstateDestination().add(id);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyedInstanceIdentifier<LinkstateRoute, LinkstateRouteKey> routeIdentifier(final InstanceIdentifier<?> id) {
        return (KeyedInstanceIdentifier<LinkstateRoute, LinkstateRouteKey>)id.firstIdentifierOf(LinkstateRoute.class);
    }

    @Override
    public CLinkstateDestination keyForIdentifier(final KeyedInstanceIdentifier<LinkstateRoute, LinkstateRouteKey> id) {
        final LinkstateRouteKey route = id.getKey();
        List<CLinkstateDestination> dests = null;
        try {
            dests = LinkstateNlriParser.parseNlri(Unpooled.wrappedBuffer(route.getRouteKey()), false);
        } catch (final BGPParsingException e) {
            LOG.warn("Unable to parse LinkstateRoute Key {}", route, e);
        }
        if (dests == null || dests.size() != 1) {
            LOG.warn("Null or more than one LinkstateRoute Key was parsed");
            return null;
        }
        return dests.get(0);
    }
}
