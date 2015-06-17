/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.CSegment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.AListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.list._case.a.list.AsSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.a.set._case.ASetBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class BestPathState {
    private static final class AttributesCollection {
        private final Collection<PathArgument> asPath;
        private final Collection<PathArgument> locPref;
        private final Collection<PathArgument> med;
        private final Collection<PathArgument> orig;

        AttributesCollection(final QName namespace) {
            NodeIdentifier container = new NodeIdentifier(QName.cachedReference(QName.create(namespace, AsPath.QNAME.getLocalName())));
            NodeIdentifier leaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "segments")));
            this.asPath = ImmutableList.<PathArgument>of(container, leaf);

            container = new NodeIdentifier(QName.cachedReference(QName.create(namespace, LocalPref.QNAME.getLocalName())));
            leaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "pref")));
            this.locPref = ImmutableList.<PathArgument>of(container, leaf);

            container = new NodeIdentifier(QName.cachedReference(QName.create(namespace, MultiExitDisc.QNAME.getLocalName())));
            leaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "med")));
            this.med = ImmutableList.<PathArgument>of(container, leaf);

            container = new NodeIdentifier(QName.cachedReference(QName.create(namespace, Origin.QNAME.getLocalName())));
            leaf = new NodeIdentifier(QName.cachedReference(QName.create(namespace, "value")));
            this.orig = ImmutableList.<PathArgument>of(container, leaf);
        }

        Collection<PathArgument> getAsPath() {
            return this.asPath;
        }

        Collection<PathArgument> getLocPref() {
            return this.locPref;
        }

        Collection<PathArgument> getMed() {
            return this.med;
        }

        Collection<PathArgument> getOrig() {
            return this.orig;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BestPathState.class);
    private static final Cache<QNameModule, AttributesCollection> PATH_CACHE = CacheBuilder.newBuilder().weakKeys().weakValues().build();

    @VisibleForTesting
    public static final NodeIdentifier C_SEGMENT_NID = new NodeIdentifier(CSegment.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier A_SET_NID = new NodeIdentifier(ASet.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier AS_SET_NID = new NodeIdentifier(QName.cachedReference(QName.create(ASet.QNAME, "as-set")));
    @VisibleForTesting
    public static final NodeIdentifier A_LIST_NID = new NodeIdentifier(AList.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier AS_SEQ_NID = new NodeIdentifier(AsSequence.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier AS_NID = new NodeIdentifier(QName.create(AsSequence.QNAME, "as"));

    private long peerAs = 0L;
    private int asPathLength = 0;

    private final ContainerNode attributes;
    private final AttributesCollection collection;
    private Long localPref;
    private Long multiExitDisc;
    private BgpOrigin origin;
    private boolean resolved;

    BestPathState(final ContainerNode attributes) {
        final AttributesCollection col;
        try {
            col = PATH_CACHE.get(attributes.getNodeType().getModule(), new Callable<AttributesCollection>() {
                @Override
                public AttributesCollection call() {
                    return new AttributesCollection(attributes.getNodeType());
                }
            });
        } catch (final ExecutionException e) {
            LOG.error("Error creating namespace-specific attributes collection.", e);
            throw new IllegalStateException("Error creating namespace-specific attributes collection.", e);
        }

        this.attributes = Preconditions.checkNotNull(attributes);
        this.collection = col;
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
            throw new IllegalArgumentException("Unhandled origin value " + originStr);
        }
    }

    private void resolveValues() {
        if (this.resolved) {
            return;
        }

        final Optional<NormalizedNode<?, ?>> maybeLocalPref = NormalizedNodes.findNode(this.attributes, this.collection.getLocPref());
        if (maybeLocalPref.isPresent()) {
            this.localPref = (Long) ((LeafNode<?>)maybeLocalPref.get()).getValue();
        } else {
            this.localPref = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeMultiExitDisc = NormalizedNodes.findNode(this.attributes, this.collection.getMed());
        if (maybeMultiExitDisc.isPresent()) {
            this.multiExitDisc = (Long) ((LeafNode<?>)maybeMultiExitDisc.get()).getValue();
        } else {
            this.multiExitDisc = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeOrigin = NormalizedNodes.findNode(this.attributes, this.collection.getOrig());
        if (maybeOrigin.isPresent()) {
            this.origin = fromString((String) ((LeafNode<?>)maybeOrigin.get()).getValue());
        } else {
            this.origin = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeSegments = NormalizedNodes.findNode(this.attributes, this.collection.getAsPath());
        if (maybeSegments.isPresent()) {
            final UnkeyedListNode segments = (UnkeyedListNode) maybeSegments.get();
            final List<Segments> segs = extractSegments(segments);
            if (segs.size() != 0) {
                this.peerAs = getPeerAs(segs).getValue();
                this.asPathLength = countAsPath(segs);
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
        return this.peerAs;
    }

    int getAsPathLength() {
        resolveValues();
        return this.asPathLength;
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

    @VisibleForTesting
    public static List<Segments> extractSegments(final UnkeyedListNode segments) {
        // list segments
        final List<Segments> extracted = new ArrayList<>();
        for (final UnkeyedListEntryNode seg : segments.getValue()) {
            CSegment cs = null;
            // choice c-segment
            final ChoiceNode segmentType = (ChoiceNode) seg.getChild(C_SEGMENT_NID).get();
            if (segmentType.getChild(A_SET_NID).isPresent()) {
                // container a-set
                cs = extractAsSet(segmentType.getChild(A_SET_NID).get());
            } else if (segmentType.getChild(A_LIST_NID).isPresent()) {
                // container a-list
                cs = extractAsSequence(segmentType.getChild(A_LIST_NID).get());
            }
            extracted.add(new SegmentsBuilder().setCSegment(cs).build());
        }
        return extracted;
    }

    private static CSegment extractAsSet(final DataContainerChild<? extends PathArgument, ?> set) {
        final List<AsNumber> ases = new ArrayList<>();
        // leaf-list a-set
        final Optional<NormalizedNode<?, ?>> maybeSet = NormalizedNodes.findNode(set, AS_SET_NID);
        if (maybeSet.isPresent()) {
            final LeafSetNode<?> list = (LeafSetNode<?>)maybeSet.get();
            for (final LeafSetEntryNode<?> as : list.getValue())  {
                ases.add(new AsNumber(Long.valueOf((String)as.getValue())));
            }
        }
        return new ASetCaseBuilder().setASet(new ASetBuilder().setAsSet(ases).build()).build();
    }

    private static CSegment extractAsSequence(final DataContainerChild<? extends PathArgument, ?> set) {
        final List<AsSequence> ases = new ArrayList<>();
        // list as-sequence
        final Optional<NormalizedNode<?, ?>> maybeSet = NormalizedNodes.findNode(set, AS_SEQ_NID);
        if (maybeSet.isPresent()) {
            final UnkeyedListNode list = (UnkeyedListNode)maybeSet.get();
            // as-sequence
            for (final UnkeyedListEntryNode as : list.getValue())  {
                // as
                final Optional<NormalizedNode<?, ?>> maybeAsSeq = NormalizedNodes.findNode(as, AS_NID);
                if (maybeAsSeq.isPresent()) {
                    final LeafNode<?> asLeaf = (LeafNode<?>)maybeAsSeq.get();
                    ases.add(new AsSequenceBuilder().setAs(new AsNumber(Long.valueOf((String)asLeaf.getValue()))).build());
                }
            }
        }
        return new AListCaseBuilder().setAList(new AListBuilder().setAsSequence(ases).build()).build();
    }

    ContainerNode getAttributes() {
        return this.attributes;
    }

    private ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("attributes", this.attributes);
        toStringHelper.add("localPref", this.localPref);
        toStringHelper.add("multiExitDisc", this.multiExitDisc);
        toStringHelper.add("origin", this.origin);
        toStringHelper.add("resolved", this.resolved);
        return toStringHelper;
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.attributes.hashCode();
        result = prime * result + ((this.localPref == null) ? 0 : this.localPref.hashCode());
        result = prime * result + ((this.multiExitDisc == null) ? 0 : this.multiExitDisc.hashCode());
        result = prime * result + ((this.origin == null) ? 0 : this.origin.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BestPathState)) {
            return false;
        }
        final BestPathState other = (BestPathState) obj;
        if (!this.attributes.equals(other.attributes)) {
            return false;
        }
        if (this.localPref == null) {
            if (other.localPref != null) {
                return false;
            }
        } else if (!this.localPref.equals(other.localPref)) {
            return false;
        }
        if (this.multiExitDisc == null) {
            if (other.multiExitDisc != null) {
                return false;
            }
        } else if (!this.multiExitDisc.equals(other.multiExitDisc)) {
            return false;
        }
        if (this.origin != other.origin) {
            return false;
        }
        return true;
    }
}
