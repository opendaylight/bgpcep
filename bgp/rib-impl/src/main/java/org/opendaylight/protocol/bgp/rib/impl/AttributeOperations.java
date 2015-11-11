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
import java.util.Collection;
import java.util.Iterator;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.CSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
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
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
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
                    return ImmutableSet.of(QName.cachedReference(QName.create(key, Origin.QNAME.getLocalName())),
                        QName.cachedReference(QName.create(key, AsPath.QNAME.getLocalName())),
                        QName.cachedReference(QName.create(key, CNextHop.QNAME.getLocalName())),
                        QName.cachedReference(QName.create(key, Communities.QNAME.getLocalName())),
                        QName.cachedReference(QName.create(key, ExtendedCommunities.QNAME.getLocalName())));
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
    private final NodeIdentifier asPathChoice;
    private final NodeIdentifier asPathList;
    private final NodeIdentifier asPathSequence;
    private final NodeIdentifier asPathId;
    private final NodeIdentifier transitiveLeaf;

    private AttributeOperations(final QNameModule namespace) {
        this.asPathContainer = new NodeIdentifier(QName.cachedReference(QName.create(namespace, AsPath.QNAME.getLocalName())));
        this.asPathSegments = new NodeIdentifier(QName.cachedReference(QName.create(namespace, Segments.QNAME.getLocalName())));
        this.asPathChoice = new NodeIdentifier(QName.cachedReference(QName.create(namespace, CSegment.QNAME.getLocalName())));
        this.asPathList = new NodeIdentifier(QName.cachedReference(QName.create(namespace, AList.QNAME.getLocalName())));
        this.asPathSequence = new NodeIdentifier(QName.cachedReference(QName.create(namespace, AsSequence.QNAME.getLocalName())));
        this.asPathId = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "as")));

        this.clusterListContainer = new NodeIdentifier(QName.cachedReference(QName.create(namespace, ClusterId.QNAME.getLocalName())));
        this.clusterQname = QName.cachedReference(QName.create(namespace, "cluster"));
        this.clusterListLeaf = new NodeIdentifier(this.clusterQname);
        this.clusterListPath = ImmutableList.<PathArgument>of(this.clusterListContainer, this.clusterListLeaf);
        this.originatorIdContainer = new NodeIdentifier(QName.cachedReference(QName.create(namespace, OriginatorId.QNAME.getLocalName())));
        this.originatorIdLeaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "originator")));
        this.originatorIdPath = ImmutableList.<PathArgument>of(this.originatorIdContainer, this.originatorIdLeaf);

        this.transitiveLeaf = new NodeIdentifier(QName.cachedReference(QName.create(UnrecognizedAttributes.QNAME, "transitive")));
        this.transitiveCollection = TRANSITIVE_CACHE.getUnchecked(namespace);
    }

    static AttributeOperations getInstance(final ContainerNode attributes) {
        return ATTRIBUTES_CACHE.getUnchecked(attributes.getNodeType().getModule());
    }

    private Collection<UnkeyedListEntryNode> reusableSequence(final UnkeyedListEntryNode segment) {
        final Optional<NormalizedNode<?, ?>> maybeAsSequence = NormalizedNodes.findNode(segment, this.asPathChoice, this.asPathList, this.asPathSequence);
        if (maybeAsSequence.isPresent()) {
            final UnkeyedListNode asList = (UnkeyedListNode) maybeAsSequence.get();
            if (asList.getSize() < Values.UNSIGNED_BYTE_MAX_VALUE) {
                return asList.getValue();
            }
        }

        return null;
    }

    ContainerNode exportedAttributes(final ContainerNode attributes, final Long localAs) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder();
        b.withNodeIdentifier(attributes.getIdentifier());

        // First filter out non-transitive attributes
        // FIXME: removes MULTI_EXIT_DISC, too.
        spliceTransitives(b, attributes);

        /*
         * This is very ugly, as our AS_PATH model is needlessly complex. The top-level container contains a list
         * of segments, each of which is a choice containing another list. Both are unkeyed lists, so we need to
         * perform a wholesale replace.
         */
        final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> sb = Builders.unkeyedListBuilder();
        sb.withNodeIdentifier(this.asPathSegments);

        final Optional<NormalizedNode<?, ?>> maybeOldAsSegments = NormalizedNodes.findNode(attributes, this.asPathContainer, this.asPathSegments);
        if (maybeOldAsSegments.isPresent() && !((UnkeyedListNode) maybeOldAsSegments.get()).getValue().isEmpty()) {
            // Builder of inner list
            final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> ilb = Builders.unkeyedListBuilder();
            ilb.withNodeIdentifier(this.asPathSequence);
            ilb.withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(this.asPathSequence).withChild(ImmutableNodes.leafNode(this.asPathId, localAs)).build());

            /*
             * We need to check the first entry in the outer list, to check if the choice is a-list. If it is and its
             * total number of elements is less than 255, we need to modify that one. Otherwise we need to create a
             * new entry.
             */
            final Iterator<UnkeyedListEntryNode> oldAsSegments = ((UnkeyedListNode) maybeOldAsSegments.get()).getValue().iterator();
            final UnkeyedListEntryNode firstSegment = oldAsSegments.next();
            final Collection<UnkeyedListEntryNode> reusable = reusableSequence(firstSegment);
            if (reusable != null) {
                for (final UnkeyedListEntryNode child : reusable) {
                    ilb.withChild(child);
                }
            }

            // Builder of inner container
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> icb = Builders.containerBuilder();
            icb.withNodeIdentifier(this.asPathList);
            icb.withChild(ilb.build());

            // Choice inside the outer list
            final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> ocb = Builders.choiceBuilder();
            ocb.withNodeIdentifier(this.asPathChoice);
            ocb.withChild(icb.build());

            // Add the new first segment
            sb.withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(this.asPathSegments).withChild(ocb.build()).build());

            // If we did not merge into the original segment, add it
            if (reusable == null) {
                sb.withChild(firstSegment);
            }

            // Add all subsequent segments
            while (oldAsSegments.hasNext()) {
                sb.withChild(oldAsSegments.next());
            }
        } else {
            // Segments are completely empty, create a completely new AS_PATH container with
            // a single entry
            sb.withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(this.asPathSegments).withChild(
                Builders.choiceBuilder().withNodeIdentifier(this.asPathChoice).withChild(
                    Builders.containerBuilder().withNodeIdentifier(this.asPathList).withChild(
                        Builders.unkeyedListBuilder().withNodeIdentifier(this.asPathSequence).withChild(
                            Builders.unkeyedListEntryBuilder().withNodeIdentifier(this.asPathSequence).withChild(
                                ImmutableNodes.leafNode(this.asPathId, localAs)).build()).build()).build()).build()).build());

        }

        b.withChild(Builders.containerBuilder().withNodeIdentifier(this.asPathContainer).withChild(sb.build()).build());
        return b.build();
    }

    /**
     * Attributes when reflecting a route from Internal iBGP(Application Peer)
     * @param attributes
     * @return
     */
    ContainerNode reflectedAttributes(final ContainerNode attributes) {
        // if there was a CLUSTER_LIST attribute, add all other entries
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, this.clusterListPath);
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder(attributes);
        final ContainerNode clusterList = createClusterList(maybeClusterList);
        if(clusterList == null) {
            return null;
        }

        return  b.withChild(clusterList).build();
    }

    private ContainerNode createClusterList(final Optional<NormalizedNode<?, ?>> maybeClusterList) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb;
        if (maybeClusterList.isPresent()) {
            // Create a new CLUSTER_LIST builder
            final ListNodeBuilder<Object, LeafSetEntryNode<Object>> clb = Builders.orderedLeafSetBuilder();
            clb.withNodeIdentifier(this.clusterListLeaf);
            final NormalizedNode<?, ?> clusterList = maybeClusterList.get();
            if (clusterList instanceof LeafSetNode) {
                for (final LeafSetEntryNode<?> n : ((LeafSetNode<?>) clusterList).getValue()) {
                    // There's no way we can safely avoid this cast
                    @SuppressWarnings("unchecked")
                    final LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>) n;
                    clb.addChild(child);
                }
                // Now wrap it in a container and add it to attributes
                cb = Builders.containerBuilder();
                cb.withNodeIdentifier(this.clusterListContainer);
                cb.withChild(clb.build());
                return cb.build();
            } else {
                LOG.warn("Ignoring malformed CLUSTER_LIST {}", clusterList);
            }
        }
        return null;
    }

    // Attributes when reflecting a route
    ContainerNode reflectedAttributes(final ContainerNode attributes, final Ipv4Address originatorId, final ClusterIdentifier clusterId) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder(attributes);

        // Create a new CLUSTER_LIST builder
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> clb = Builders.orderedLeafSetBuilder();
        clb.withNodeIdentifier(this.clusterListLeaf);

        // prepend local CLUSTER_ID
        clb.withChild(Builders.leafSetEntryBuilder().withNodeIdentifier(new NodeWithValue(this.clusterQname, clusterId.getValue())).withValue(clusterId.getValue()).build());

        // if there was a CLUSTER_LIST attribute, add all other entries
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, this.clusterListPath);
        if (maybeClusterList.isPresent()) {
            final NormalizedNode<?, ?> clusterList = maybeClusterList.get();
            if (clusterList instanceof LeafSetNode) {
                for (final LeafSetEntryNode<?> n : ((LeafSetNode<?>)clusterList).getValue()) {
                    // There's no way we can safely avoid this cast
                    @SuppressWarnings("unchecked")
                    final LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>)n;
                    clb.addChild(child);
                }
            } else {
                LOG.warn("Ignoring malformed CLUSTER_LIST {}", clusterList);
            }
        } else {
            LOG.debug("Creating fresh CLUSTER_LIST attribute");
        }

        // Now wrap it in a container and add it to attributes
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder();
        cb.withNodeIdentifier(this.clusterListContainer);
        cb.withChild(clb.build());
        b.withChild(cb.build());

        // add ORIGINATOR_ID if not present
        final Optional<NormalizedNode<?, ?>> maybeOriginatorId = NormalizedNodes.findNode(attributes, this.originatorIdPath);
        if (!maybeOriginatorId.isPresent()) {
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> oib = Builders.containerBuilder();
            oib.withNodeIdentifier(this.originatorIdContainer);
            oib.withChild(ImmutableNodes.leafNode(this.originatorIdLeaf, originatorId.getValue()));
            b.withChild(oib.build());
        }

        return b.build();
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
            return ((LeafNode<?>) originatorId).getValue();
        }

        LOG.warn("Unexpected ORIGINATOR_ID node {}, ignoring it", originatorId);
        return null;
    }
}
