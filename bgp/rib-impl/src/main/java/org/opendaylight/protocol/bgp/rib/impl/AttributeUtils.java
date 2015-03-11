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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with route attributes in binding independent form. An instance
 * is always bound to a certain namespace, so it maintains cached attribute identifiers.
 */
final class AttributeUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AttributeUtils.class);
    private static final LoadingCache<QNameModule, AttributeUtils> ATTRIBUTES_CACHE = CacheBuilder.newBuilder().weakKeys().weakValues().build(
        new CacheLoader<QNameModule, AttributeUtils>() {
            @Override
            public AttributeUtils load(final QNameModule key) throws Exception {
                return new AttributeUtils(key);
            }
        }
    );

    private final Iterable<PathArgument> originatorIdPath;
    private final Iterable<PathArgument> clusterListPath;
    private final NodeIdentifier originatorIdContainer;
    private final NodeIdentifier originatorIdLeaf;
    private final NodeIdentifier clusterListContainer;
    private final NodeIdentifier clusterListLeaf;

    private AttributeUtils(final QNameModule namespace) {
        this.clusterListContainer = new NodeIdentifier(QName.cachedReference(QName.create(namespace, ClusterId.QNAME.getLocalName())));
        this.clusterListLeaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "cluster")));
        this.clusterListPath = ImmutableList.<PathArgument>of(clusterListContainer, clusterListLeaf);
        this.originatorIdContainer = new NodeIdentifier(QName.cachedReference(QName.create(namespace, OriginatorId.QNAME.getLocalName())));
        this.originatorIdLeaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "originator")));
        this.originatorIdPath = ImmutableList.<PathArgument>of(originatorIdContainer, originatorIdLeaf);
    }

    static AttributeUtils getInstance(final QNameModule namespace) {
        return ATTRIBUTES_CACHE.getUnchecked(namespace);
    }

    // Attributes when reflecting a route
    ContainerNode reflectedAttributes(final ContainerNode attributes, final Ipv4Address originatorId, final ClusterIdentifier clusterId) {
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> b = Builders.containerBuilder(attributes);

        // Create a new CLUSTER_LIST builder
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> clb = Builders.leafSetBuilder();
        clb.withNodeIdentifier(clusterListLeaf);

        // prepend local CLUSTER_ID
        clb.withChild(Builders.leafSetEntryBuilder().withValue(clusterId).build());

        // if there was a CLUSTER_LIST attribute, add all other entries
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, clusterListPath);
        if (maybeClusterList.isPresent()) {
            final NormalizedNode<?, ?> clusterList = maybeClusterList.get();
            if (clusterList instanceof LeafSetNode) {
                // FIXME: can we get rid of this cast?
                for (LeafSetEntryNode<Object> e : ((LeafSetNode<Object>)clusterList).getValue()) {
                    clb.addChild(e);
                }
            } else {
                LOG.warn("Ignoring malformed CLUSTER_LIST {}", clusterList);
            }
        } else {
            LOG.debug("Creating fresh CLUSTER_LIST attribute");
        }

        // Now wrap it in a container and add it to attributes
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> cb = Builders.containerBuilder();
        cb.withNodeIdentifier(clusterListContainer);
        cb.withChild(clb.build());
        b.withChild(cb.build());

        // add ORIGINATOR_ID if not present
        final Optional<NormalizedNode<?, ?>> maybeOriginatorId = NormalizedNodes.findNode(attributes, originatorIdPath);
        if (!maybeOriginatorId.isPresent()) {
            final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> oib = Builders.containerBuilder();
            oib.withNodeIdentifier(originatorIdContainer);
            oib.withChild(ImmutableNodes.leafNode(originatorIdLeaf, originatorId.getValue()));
            b.withChild(oib.build());
        }

        return b.build();
    }

    LeafSetNode<?> getClusterList(final ContainerNode attributes) {
        final Optional<NormalizedNode<?, ?>> maybeClusterList = NormalizedNodes.findNode(attributes, clusterListPath);
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
        final Optional<NormalizedNode<?, ?>> maybeOriginatorId = NormalizedNodes.findNode(attributes, originatorIdPath);
        if (!maybeOriginatorId.isPresent()) {
            LOG.debug("No ORIGINATOR_ID present");
            return null;
        }

        final NormalizedNode<?, ?> originatorId = maybeOriginatorId.get();
        if (originatorId instanceof LeafNode) {
            return ((LeafNode<?>)originatorId).getValue();
        }

        LOG.warn("Unexpected ORIGINATOR_ID node {}, ignoring it", originatorId);
        return null;
    }
}
