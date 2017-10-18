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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeCachingCodec;
import org.opendaylight.protocol.bgp.rib.impl.spi.Codecs;
import org.opendaylight.protocol.bgp.rib.spi.RIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ExtendedCommunity;
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
    private static final InstanceIdentifier<MpReachNlri> MP_REACH_NLRI_II = InstanceIdentifier.create(Update.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes.class)
                .augmentation(Attributes1.class)
                .child(MpReachNlri.class);
    private static final InstanceIdentifier<MpUnreachNlri> MP_UNREACH_NLRI_II = InstanceIdentifier.create(Update.class)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes.class)
            .augmentation(Attributes2.class)
            .child(MpUnreachNlri.class);

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

    private final ImmutableSet<Class<? extends DataObject>> cacheableAttributes;
    private BindingNormalizedNodeCachingCodec<Attributes> attributesCodec;
    private BindingNormalizedNodeCachingCodec<MpReachNlri> reachNlriCodec;
    private BindingNormalizedNodeCachingCodec<MpUnreachNlri> unreachNlriCodec;

    private final RIBSupport ribSupport;

    public CodecsImpl(final RIBSupport ribSupport) {
        this.ribSupport = requireNonNull(ribSupport);
        final Builder<Class<? extends DataObject>> acb = ImmutableSet.builder();
        acb.addAll(ATTRIBUTE_CACHEABLES);
        acb.addAll(this.ribSupport.cacheableAttributeObjects());
        this.cacheableAttributes = acb.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onCodecTreeUpdated(final BindingCodecTree tree) {

        @SuppressWarnings("rawtypes")
        final BindingCodecTreeNode tableCodecContext = tree.getSubtreeCodec(TABLE_BASE_II);
        final BindingCodecTreeNode<? extends Route> routeListCodec = tableCodecContext
            .streamChild(Routes.class)
            .streamChild(this.ribSupport.routesCaseClass())
            .streamChild(this.ribSupport.routesContainerClass())
            .streamChild(this.ribSupport.routesListClass());

        this.attributesCodec = routeListCodec.streamChild(Attributes.class).createCachingCodec(this.cacheableAttributes);
        this.reachNlriCodec = tree.getSubtreeCodec(MP_REACH_NLRI_II).createCachingCodec(this.ribSupport.cacheableNlriObjects());
        this.unreachNlriCodec = tree.getSubtreeCodec(MP_UNREACH_NLRI_II).createCachingCodec(this.ribSupport.cacheableNlriObjects());
    }

    @Override
    public ContainerNode serializeUnreachNlri(final MpUnreachNlri nlri) {
        Preconditions.checkState(this.unreachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) this.unreachNlriCodec.serialize(nlri);
    }

    @Override
    public ContainerNode serializeReachNlri(final MpReachNlri nlri) {
        Preconditions.checkState(this.reachNlriCodec != null, "MpReachNlri codec not available");
        return (ContainerNode) this.reachNlriCodec.serialize(nlri);
    }

    @Override
    public Attributes deserializeAttributes(final NormalizedNode<?,?> attributes) {
        Preconditions.checkState(this.attributesCodec != null, "Attributes codec not available");
        return this.attributesCodec.deserialize(attributes);
    }

    @Override
    public ContainerNode serializeAttributes(final Attributes pathAttr) {
        Preconditions.checkState(this.attributesCodec != null, "Attributes codec not available");
        final AttributesBuilder a = new AttributesBuilder(pathAttr);
        a.addAugmentation(Attributes1.class, null);
        a.addAugmentation(Attributes2.class, null);
        return (ContainerNode) this.attributesCodec.serialize(a.build());
    }
}
