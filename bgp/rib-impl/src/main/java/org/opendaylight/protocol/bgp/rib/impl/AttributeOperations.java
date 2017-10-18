/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with route attributes in binding independent form. An instance
 * is always bound to a certain namespace, so it maintains cached attribute identifiers.
 */
final class AttributeOperations {
    private static final Logger LOG = LoggerFactory.getLogger(AttributeOperations.class);
    private static final LoadingCache<QNameModule, AttributeOperations> ATTRIBUTES_CACHE = CacheBuilder.newBuilder().weakKeys().weakValues().build(
        new CacheLoader<QNameModule, AttributeOperations>() {
            @Override
            public AttributeOperations load(final QNameModule key) throws Exception {
                return new AttributeOperations(key);
            }
        });
    private static final LoadingCache<QNameModule, ImmutableSet<QName>> TRANSITIVE_CACHE = CacheBuilder.newBuilder()
        .weakKeys()
        .weakValues().build(
            new CacheLoader<QNameModule, ImmutableSet<QName>>() {
                @Override
                public ImmutableSet<QName> load(final QNameModule key) {
                    return ImmutableSet.of(QName.create(key, Origin.QNAME.getLocalName()).intern(),
                        QName.create(key, AsPath.QNAME.getLocalName()).intern(),
                        QName.create(key, CNextHop.QNAME.getLocalName()).intern(),
                        QName.create(key, Communities.QNAME.getLocalName()).intern(),
                        QName.create(key, ExtendedCommunities.QNAME.getLocalName()).intern());
                }
            });
    private final ImmutableSet<QName> transitiveCollection;
    private final Iterable<PathArgument> originatorIdPath;
    private final Iterable<PathArgument> clusterListPath;
    private final NodeIdentifier originatorIdContainer;
    private final NodeIdentifier originatorIdLeaf;
    private final NodeIdentifier clusterListContainer;
    private final NodeIdentifier clusterListLeaf;
    private final QName clusterQname;
    private final NodeIdentifier asPathContainer;
    private final NodeIdentifier asPathSegments;
    private final NodeIdentifier asPathSequence;
    private final QName asNumberQname;
    private final NodeIdentifier transitiveLeaf;

    private AttributeOperations(final QNameModule namespace) {
        this.asPathContainer = new NodeIdentifier(QName.create(namespace, AsPath.QNAME.getLocalName()).intern());
        this.asPathSegments = new NodeIdentifier(QName.create(namespace, Segments.QNAME.getLocalName()).intern());
        this.asPathSequence = new NodeIdentifier(QName.create(namespace, "as-sequence").intern());
        this.asNumberQname = QName.create(namespace, "as-number").intern();

        this.clusterListContainer = new NodeIdentifier(QName.create(namespace, ClusterId.QNAME.getLocalName()).intern());
        this.clusterQname = QName.create(namespace, "cluster").intern();
        this.clusterListLeaf = new NodeIdentifier(this.clusterQname);
        this.clusterListPath = ImmutableList.of(this.clusterListContainer, this.clusterListLeaf);
        this.originatorIdContainer = new NodeIdentifier(QName.create(namespace, OriginatorId.QNAME.getLocalName()).intern());
        this.originatorIdLeaf = new NodeIdentifier(QName.create(namespace, "originator").intern());
        this.originatorIdPath = ImmutableList.of(this.originatorIdContainer, this.originatorIdLeaf);

        this.transitiveLeaf = new NodeIdentifier(QName.create(UnrecognizedAttributes.QNAME, "transitive").intern());
        this.transitiveCollection = TRANSITIVE_CACHE.getUnchecked(namespace);
    }

    static AttributeOperations getInstance(final ContainerNode attributes) {
        return ATTRIBUTES_CACHE.getUnchecked(attributes.getNodeType().getModule());
    }

    private LeafSetNode<?> reusableSegment(final UnkeyedListEntryNode segment) {
        final Optional<NormalizedNode<?, ?>> maybeAsSequence = NormalizedNodes.findNode(segment, this.asPathSequence);
        if (maybeAsSequence.isPresent()) {
            final LeafSetNode<?> asList = (LeafSetNode<?>) maybeAsSequence.get();
            if (asList.getValue().size() < Values.UNSIGNED_BYTE_MAX_VALUE) {
                return asList;
            }
        }
        return null;
    }

    ContainerNode exportedAttributes(final ContainerNode attributes, final Long localAs) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBuilder = Builders.containerBuilder();
        containerBuilder.withNodeIdentifier(attributes.getIdentifier());

        // First filter out non-transitive attributes
        // FIXME: removes MULTI_EXIT_DISC, too.
        spliceTransitives(containerBuilder, attributes);

        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> segmentsBuilder = Builders.unkeyedListBuilder();
        segmentsBuilder.withNodeIdentifier(this.asPathSegments);

        final Optional<NormalizedNode<?, ?>> maybeOldAsSegments = NormalizedNodes.findNode(attributes, this.asPathContainer, this.asPathSegments);
        if (maybeOldAsSegments.isPresent() && !((UnkeyedListNode) maybeOldAsSegments.get()).getValue().isEmpty()) {

            /*
             * We need to check the first segment.
             * If it has as-set then new as-sequence with local AS is prepended.
             * If it has as-sequence, we may add local AS when it has less than 255 elements.
             * Otherwise we need to create new as-sequence for local AS.
             */

            final ListNodeBuilder<Object,LeafSetEntryNode<Object>> asSequenceBuilder = Builders.orderedLeafSetBuilder();
            // add local AS
            asSequenceBuilder.withNodeIdentifier(this.asPathSequence).addChild(Builders.leafSetEntryBuilder().withNodeIdentifier(
                new NodeWithValue<>(this.asNumberQname, localAs)).withValue(localAs).build());

            final Iterator<UnkeyedListEntryNode> oldAsSegments = ((UnkeyedListNode) maybeOldAsSegments.get()).getValue().iterator();
            final UnkeyedListEntryNode firstSegment = oldAsSegments.next();
            final LeafSetNode<?> reusableAsSeq = reusableSegment(firstSegment);
            // first segment contains as-sequence with less then 255 elements and it's append to local AS
            if (reusableAsSeq != null) {
                for (final LeafSetEntryNode<?> child : reusableAsSeq.getValue())  {
                    asSequenceBuilder.withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue<>(
                            this.asNumberQname, child.getValue())).withValue(child.getValue()).build());
                }
            }
            // Add the new first segment
            segmentsBuilder.withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(this.asPathSegments).withChild(asSequenceBuilder.build()).build());

            // When first segment contains as-set or full as-sequence, append it
            if (reusableAsSeq == null) {
                segmentsBuilder.withChild(firstSegment);
            }

            // Add all subsequent segments
            while (oldAsSegments.hasNext()) {
                segmentsBuilder.withChild(oldAsSegments.next());
            }
        } else {
            // Segments are completely empty, create a completely new AS_PATH container with
            // a single entry
            segmentsBuilder.withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(this.asPathSegments).withChild(
                Builders.orderedLeafSetBuilder().withNodeIdentifier(this.asPathSequence).addChild(
                    Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue<>(this.asNumberQname, localAs))
                    .withValue(localAs).build()).build()).build());
        }

        containerBuilder.withChild(Builders.containerBuilder().withNodeIdentifier(this.asPathContainer).withChild(segmentsBuilder.build()).build());
        return containerBuilder.build();
    }

    /**
     * Attributes when reflecting a route from Internal iBGP (Application Peer)
     * @param attributes
     * @return
     */
    ContainerNode reflectedAttributes(final ContainerNode attributes) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> attributesContainer = Builders.containerBuilder(attributes);

        // if there was a CLUSTER_LIST attribute, add it
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, this.clusterListPath);
        if (maybeClusterList.isPresent()) {
            // Create a new CLUSTER_LIST builder
            final ListNodeBuilder<Object, LeafSetEntryNode<Object>> clusterBuilder = Builders.orderedLeafSetBuilder();
            clusterBuilder.withNodeIdentifier(this.clusterListLeaf);
            AttributeOperations.addOtherClusterEntries(maybeClusterList, clusterBuilder);
            // Now wrap it in a container and add it to attributes
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> clusterListCont= Builders.containerBuilder();
            clusterListCont.withNodeIdentifier(this.clusterListContainer);
            clusterListCont.withChild(clusterBuilder.build());
            attributesContainer.withChild(clusterListCont.build());
        }

        return attributesContainer.build();
    }

    // Attributes when reflecting a route
    ContainerNode reflectedAttributes(final ContainerNode attributes, final Ipv4Address originatorId, final ClusterIdentifier clusterId) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> attributesContainer = Builders.containerBuilder(attributes);

        // Create a new CLUSTER_LIST builder
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> clusterBuilder = Builders.orderedLeafSetBuilder();
        clusterBuilder.withNodeIdentifier(this.clusterListLeaf);

        // prepend local CLUSTER_ID
        clusterBuilder.withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue<>(
                this.clusterQname, clusterId.getValue())).withValue(clusterId.getValue()).build());

        // if there was a CLUSTER_LIST attribute, add all other entries
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, this.clusterListPath);
        if (maybeClusterList.isPresent()) {
            AttributeOperations.addOtherClusterEntries(maybeClusterList, clusterBuilder);
        } else {
            LOG.debug("Creating fresh CLUSTER_LIST attribute");
        }

        // Now wrap it in a container and add it to attributes
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> clusterListCont = Builders.containerBuilder();
        clusterListCont.withNodeIdentifier(this.clusterListContainer);
        clusterListCont.withChild(clusterBuilder.build());
        attributesContainer.withChild(clusterListCont.build());

        // add ORIGINATOR_ID if not present
        final Optional<NormalizedNode<?, ?>> maybeOriginatorId = NormalizedNodes.findNode(attributes, this.originatorIdPath);
        if (!maybeOriginatorId.isPresent()) {
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> originatorIDBuilder = Builders.containerBuilder();
            originatorIDBuilder.withNodeIdentifier(this.originatorIdContainer);
            originatorIDBuilder.withChild(ImmutableNodes.leafNode(this.originatorIdLeaf, originatorId.getValue()));
            attributesContainer.withChild(originatorIDBuilder.build());
        }

        return attributesContainer.build();
    }

    private static void addOtherClusterEntries(final Optional<NormalizedNode<?, ?>> maybeClusterList, final ListNodeBuilder<Object,
        LeafSetEntryNode<Object>> clb) {
        final NormalizedNode<?, ?> clusterList = maybeClusterList.get();
        if (clusterList instanceof LeafSetNode) {
            for (final LeafSetEntryNode<?> n : ((LeafSetNode<?>) clusterList).getValue()) {
                // There's no way we can safely avoid this cast
                @SuppressWarnings("unchecked")
                final LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>) n;
                clb.addChild(child);
            }
        } else {
            LOG.warn("Ignoring malformed CLUSTER_LIST {}", clusterList);
        }
    }

    private boolean isTransitiveAttribute(final DataContainerChild<? extends PathArgument, ?> child) {
        if (child.getIdentifier() instanceof AugmentationIdentifier) {
            final AugmentationIdentifier ai = (AugmentationIdentifier) child.getIdentifier();
            for (final QName name : ai.getPossibleChildNames()) {
                LOG.trace("Augmented QNAME {}", name);
                if (this.transitiveCollection.contains(name)) {
                    return true;
                }
            }
            return false;
        }
        if (this.transitiveCollection.contains(child.getNodeType())) {
            return true;
        }
        if (UnrecognizedAttributes.QNAME.equals(child.getNodeType())) {
            final Optional<NormalizedNode<?, ?>> maybeTransitive = NormalizedNodes.findNode(child, this.transitiveLeaf);
            if (maybeTransitive.isPresent()) {
                return (Boolean) maybeTransitive.get().getValue();
            }
        }
        return false;
    }

    private boolean spliceTransitives(final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> target, final ContainerNode attributes) {
        // We want to reuse attributes as much as possible, so the result of the loop
        // indicates whether we performed a modification. If we have not modified the
        // attributes, we can reuse them.
        boolean ret = false;
        for (final DataContainerChild<? extends PathArgument, ?> child : attributes.getValue()) {
            if (isTransitiveAttribute(child)) {
                target.withChild(child);
            } else {
                ret = true;
            }
        }

        return ret;
    }

    /**
     * Filter out all non-transitive attributes.
     *
     * @param attributes Input attributes
     * @return Output attributes, transitive only.
     */
    ContainerNode transitiveAttributes(final ContainerNode attributes) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(attributes.getIdentifier());

        final boolean modified = spliceTransitives(b, attributes);
        return modified ? b.build() : attributes;
    }

    LeafSetNode<?> getClusterList(final ContainerNode attributes) {
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, this.clusterListPath);
        if (maybeClusterList.isPresent()) {
            final NormalizedNode<?, ?> clusterList = maybeClusterList.get();
            if (clusterList instanceof LeafSetNode) {
                return (LeafSetNode<?>) clusterList;
            }

            LOG.warn("Unexpected CLUSTER_LIST node {}, ignoring it", clusterList);
        }

        return null;
    }

    Object getOriginatorId(final ContainerNode attributes) {
        final Optional<NormalizedNode<?, ?>> maybeOriginatorId = NormalizedNodes.findNode(attributes, this.originatorIdPath);
        if (!maybeOriginatorId.isPresent()) {
            LOG.debug("No ORIGINATOR_ID present");
            return null;
        }

        final NormalizedNode<?, ?> originatorId = maybeOriginatorId.get();
        if (originatorId instanceof LeafNode) {
            return originatorId.getValue();
        }

        LOG.warn("Unexpected ORIGINATOR_ID node {}, ignoring it", originatorId);
        return null;
    }
}
