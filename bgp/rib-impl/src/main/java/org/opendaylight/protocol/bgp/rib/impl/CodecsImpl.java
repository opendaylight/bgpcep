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
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Set;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingDataObjectCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeCachingCodec;
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
import org.opendaylight.yangtools.yang.binding.BindingObject;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public final class CodecsImpl implements Codecs {

    private static final Set<Class<? extends DataObject>> ATTRIBUTE_CACHEABLES;
    private static final InstanceIdentifier<Tables> TABLE_BASE_II = InstanceIdentifier.builder(BgpRib.class)
            .child(Rib.class)
            .child(LocRib.class)
            .child(Tables.class)
            .build();

    static {
        final Builder<Class<? extends DataObject>> acb = ImmutableSet.builder();
        acb.add(Aggregator.class);
        acb.add(BgpAggregator.class);
        acb.add(AsPath.class);
        acb.add(ClusterId.class);
        acb.add(Community.class);
        acb.add(Communities.class);
        acb.add(ExtendedCommunity.class);
        acb.add(ExtendedCommunities.class);
        acb.add(LocalPref.class);
        acb.add(MultiExitDisc.class);
        acb.add(Origin.class);
        acb.add(OriginatorId.class);
        acb.add(UnrecognizedAttributes.class);
        ATTRIBUTE_CACHEABLES = acb.build();
    }

    private final ImmutableSet<Class<? extends BindingObject>> cacheableAttributes;
    private BindingNormalizedNodeCachingCodec<Attributes> attributesCodec;
    private BindingNormalizedNodeCachingCodec<MpReachNlri> reachNlriCodec;
    private BindingNormalizedNodeCachingCodec<MpUnreachNlri> unreachNlriCodec;

    private final RIBSupport<?, ?> ribSupport;

    public CodecsImpl(final RIBSupport<?, ?> ribSupport) {
        this.ribSupport = requireNonNull(ribSupport);
        final Builder<Class<? extends BindingObject>> acb = ImmutableSet.builder();
        acb.addAll(ATTRIBUTE_CACHEABLES);
        acb.addAll(this.ribSupport.cacheableAttributeObjects());
        cacheableAttributes = acb.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCodecTreeUpdated(final BindingCodecTree tree) {

        @SuppressWarnings("rawtypes")
        final BindingDataObjectCodecTreeNode tableCodecContext = tree.getSubtreeCodec(TABLE_BASE_II);
        final BindingDataObjectCodecTreeNode<? extends Route> routeListCodec = tableCodecContext
            .streamChild(Routes.class)
            .streamChild(ribSupport.routesCaseClass())
            .streamChild(ribSupport.routesContainerClass())
            .streamChild(ribSupport.routesListClass());

        attributesCodec = routeListCodec.streamChild(Attributes.class)
            .createCachingCodec(cacheableAttributes);

        final var attrCodec = tree.streamChild(Update.class)
            .streamChild(Attributes.class);
        reachNlriCodec = attrCodec.streamChild(AttributesReach.class)
            .streamChild(MpReachNlri.class)
            .createCachingCodec(ribSupport.cacheableNlriObjects());
        unreachNlriCodec = attrCodec.streamChild(AttributesUnreach.class)
            .streamChild(MpUnreachNlri.class)
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
