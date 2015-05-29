/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

@NotThreadSafe
final class BestPathState {
    private final Collection<PathArgument> asPath;
    private final Collection<PathArgument> locPref;
    private final Collection<PathArgument> med;
    private final Collection<PathArgument> orig;

    private static Long peerAs = 0L;
    private static int asPathLength = 0;

    private final ContainerNode attributes;
    private Long localPref;
    private Long multiExitDisc;
    private BgpOrigin origin;
    private boolean resolved;
    private static final int MAX_SIZE = 1000;

    private static final LoadingCache<QName, NodeIdentifier> nodeTypeToNodeIdentifier = CacheBuilder.newBuilder().maximumSize(MAX_SIZE).build(new CacheLoader<QName, NodeIdentifier>() {
        @Override
        public NodeIdentifier load(QName key) throws Exception {
            return addNodeIdentifier(key);
        }
    });

    BestPathState(final ContainerNode attributes) {
        this.attributes = Preconditions.checkNotNull(attributes);
        final QName attributesNodeType = attributes.getNodeType();
        this.asPath = ImmutableList.<PathArgument>of(addNodeIdentifier(QName.create(attributesNodeType, "as-path")), addNodeIdentifier(QName.create(attributesNodeType, "segments")));
        this.locPref = ImmutableList.<PathArgument>of(addNodeIdentifier(QName.create(attributesNodeType, "local-pref")), addNodeIdentifier(QName.create(attributesNodeType, "pref")));
        this.med = ImmutableList.<PathArgument>of(addNodeIdentifier(QName.create(attributesNodeType, "multi-exit-disc")), addNodeIdentifier(QName.create(attributesNodeType, "med")));
        this.orig = ImmutableList.<PathArgument>of(addNodeIdentifier(QName.create(attributesNodeType, "origin")), addNodeIdentifier(QName.create(attributesNodeType, "value")));
    }

    private static BgpOrigin fromString(final String originStr) {
        switch (originStr) {
        case "igp":
            return BgpOrigin.Igp;
        case "egp":
            return BgpOrigin.Egp;
        case "incomplete":
            return BgpOrigin.Incomplete;
        default:
            throw new IllegalArgumentException("Unhandleed origin value " + originStr);
        }
    }

    private void resolveValues() {
        if (this.resolved) {
            return;
        }

        final Optional<NormalizedNode<?, ?>> maybeLocalPref = NormalizedNodes.findNode(this.attributes, this.locPref);
        if (maybeLocalPref.isPresent()) {
            this.localPref = (Long) ((LeafNode<?>)maybeLocalPref.get()).getValue();
        } else {
            this.localPref = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeMultiExitDisc = NormalizedNodes.findNode(this.attributes, this.med);
        if (maybeMultiExitDisc.isPresent()) {
            this.multiExitDisc = (Long) ((LeafNode<?>)maybeMultiExitDisc.get()).getValue();
        } else {
            this.multiExitDisc = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeOrigin = NormalizedNodes.findNode(this.attributes, this.orig);
        if (maybeOrigin.isPresent()) {
            this.origin = fromString((String) ((LeafNode<?>)maybeOrigin.get()).getValue());
        } else {
            this.origin = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeSegments = NormalizedNodes.findNode(this.attributes, this.asPath);
        if (maybeSegments.isPresent()) {
            final UnkeyedListNode segments = (UnkeyedListNode) maybeSegments.get();

            if (segments.getSize() != 0) {
                // FIXME: peer AS number

                // FIXME: asPathLength = countAsPath(this.bestState.getAsPath().getSegments());
                final boolean haveSegment;
                for (final UnkeyedListEntryNode s : segments.getValue()) {

                }
            }
        }

        this.resolved = true;
    }

    Long getLocalPref() {
        resolveValues();
        return this.localPref;
    }

    Long getMultiExitDisc() {
        resolveValues();
        return this.multiExitDisc;
    }

    BgpOrigin getOrigin() {
        resolveValues();
        return this.origin;
    }

    Long getPeerAs() {
        resolveValues();
        return peerAs;
    }

    int getAsPathLength() {
        resolveValues();
        return asPathLength;
    }

    private static int countAsPath(final List<Segments> segments) {
        // an AS_SET counts as 1, no matter how many ASs are in the set.
        int count = 0;
        boolean setPresent = false;
        for (final Segments s : segments) {
            if (s.getCSegment() instanceof ASetCase) {
                setPresent = true;
            } else {
                final AListCase list = (AListCase) s.getCSegment();
                count += list.getAList().getAsSequence().size();
            }
        }
        return (setPresent) ? count + 1 : count;
    }

    private static AsNumber getPeerAs(final List<Segments> segments) {
        if (segments.isEmpty()) {
            return null;
        }

        final AListCase first = (AListCase) segments.get(0).getCSegment();
        return first.getAList().getAsSequence().get(0).getAs();
    }

    ContainerNode getAttributes() {
        return this.attributes;
    }

    private static final NodeIdentifier addNodeIdentifier(final QName nodeType) {
        try {
            return nodeTypeToNodeIdentifier.get(nodeType);
        } catch (Exception e) {
            return new NodeIdentifier(nodeType);
        }
    }
}
