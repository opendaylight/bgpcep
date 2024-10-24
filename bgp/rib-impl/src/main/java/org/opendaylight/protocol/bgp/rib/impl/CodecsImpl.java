/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreach;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ExtendedCommunity;
import org.opendaylight.yangtools.binding.BindingObject;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.binding.data.codec.api.BindingCodecTree;
import org.opendaylight.yangtools.binding.data.codec.api.BindingDataObjectCodecTreeNode;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeCachingCodec;
import org.opendaylight.yangtools.binding.data.codec.api.CommonDataObjectCodecTreeNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public final class CodecsImpl implements Codecs {
    private static final Set<Class<? extends BindingObject>> ATTRIBUTE_CACHEABLES =
        ImmutableSet.<Class<? extends BindingObject>>builder()
            .add(Aggregator.class)
            .add(BgpAggregator.class)
            .add(AsPath.class)
            .add(ClusterId.class)
            .add(Community.class)
            .add(Communities.class)
            .add(ExtendedCommunity.class)
            .add(ExtendedCommunities.class)
            .add(LocalPref.class)
            .add(MultiExitDisc.class)
            .add(Origin.class)
            .add(OriginatorId.class)
            .add(UnrecognizedAttributes.class)
            .build();

    private static final DataObjectReference<Tables> TABLE_BASE_II = DataObjectReference.builder(BgpRib.class)
            .child(Rib.class)
            .child(LocRib.class)
            .child(Tables.class)
            .build();

    private final ImmutableSet<Class<? extends BindingObject>> cacheableAttributes;
    private BindingNormalizedNodeCachingCodec<Attributes> attributesCodec;
    private BindingNormalizedNodeCachingCodec<MpReachNlri> reachNlriCodec;
    private BindingNormalizedNodeCachingCodec<MpUnreachNlri> unreachNlriCodec;

    private final RIBSupport<?, ?> ribSupport;

    public CodecsImpl(final RIBSupport<?, ?> ribSupport) {
        this.ribSupport = requireNonNull(ribSupport);
        cacheableAttributes = ImmutableSet.<Class<? extends BindingObject>>builder()
            .addAll(ATTRIBUTE_CACHEABLES)
            .addAll(this.ribSupport.cacheableAttributeObjects())
            .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCodecTreeUpdated(final BindingCodecTree tree) {

        final CommonDataObjectCodecTreeNode<Tables> codecContext = tree.getSubtreeCodec(TABLE_BASE_II);
        if (!(codecContext instanceof BindingDataObjectCodecTreeNode tableCodecContext)) {
            throw new IllegalStateException("Unexpected table codec " + codecContext);
        }

        final BindingDataObjectCodecTreeNode<? extends Route> routeListCodec = tableCodecContext
            .getStreamChild(Routes.class)
            .getStreamChild(ribSupport.routesCaseClass())
            .getStreamChild(ribSupport.routesContainerClass())
            .getStreamDataObject(ribSupport.routesListClass());

        attributesCodec = routeListCodec.getStreamDataObject(Attributes.class)
            .createCachingCodec(cacheableAttributes);

        final var attrCodec = tree.getStreamChild(Update.class).getStreamChild(Attributes.class);
        reachNlriCodec = attrCodec.getStreamChild(AttributesReach.class)
            .getStreamDataObject(MpReachNlri.class)
            .createCachingCodec(ribSupport.cacheableNlriObjects());
        unreachNlriCodec = attrCodec.getStreamChild(AttributesUnreach.class)
            .getStreamDataObject(MpUnreachNlri.class)
            .createCachingCodec(ribSupport.cacheableNlriObjects());
    }

    @Override
    public ContainerNode serializeUnreachNlri(final MpUnreachNlri nlri) {
        Preconditions.checkState(unreachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) unreachNlriCodec.serialize(nlri);
    }

    @Override
    public ContainerNode serializeReachNlri(final MpReachNlri nlri) {
        Preconditions.checkState(reachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) reachNlriCodec.serialize(nlri);
    }

    @Override
    public Attributes deserializeAttributes(final NormalizedNode attributes) {
        Preconditions.checkState(attributesCodec != null, "Attributes codec not available");
        return attributesCodec.deserialize(attributes);
    }

    @Override
    public ContainerNode serializeAttributes(final Attributes pathAttr) {
        Preconditions.checkState(attributesCodec != null, "Attributes codec not available");
        final AttributesBuilder a = new AttributesBuilder(pathAttr);
        a.removeAugmentation(AttributesReach.class);
        a.removeAugmentation(AttributesUnreach.class);
        return (ContainerNode) attributesCodec.serialize(a.build());
    }
}
