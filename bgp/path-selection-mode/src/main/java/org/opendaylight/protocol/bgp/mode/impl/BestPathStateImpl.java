/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
public final class BestPathStateImpl implements BestPathState {
    private static final class NamespaceSpecificIds {
        private final Collection<PathArgument> asPath;
        private final Collection<PathArgument> locPref;
        private final Collection<PathArgument> med;
        private final Collection<PathArgument> orig;
        private final NodeIdentifier asSetNid;
        private final NodeIdentifier asSeqNid;

        NamespaceSpecificIds(final QName namespace) {
            NodeIdentifier container = new NodeIdentifier(QName.create(namespace, AsPath.QNAME.getLocalName().intern()));
            NodeIdentifier leaf = new NodeIdentifier(QName.create(namespace, "segments").intern());
            this.asPath = ImmutableList.of(container, leaf);

            container = new NodeIdentifier(QName.create(namespace, LocalPref.QNAME.getLocalName()).intern());
            leaf = new NodeIdentifier(QName.create(namespace, "pref").intern());
            this.locPref = ImmutableList.of(container, leaf);

            container = new NodeIdentifier(QName.create(namespace, MultiExitDisc.QNAME.getLocalName()).intern());
            leaf = new NodeIdentifier(QName.create(namespace, "med").intern());
            this.med = ImmutableList.of(container, leaf);

            container = new NodeIdentifier(QName.create(namespace, Origin.QNAME.getLocalName()).intern());
            leaf = new NodeIdentifier(QName.create(namespace, "value").intern());
            this.orig = ImmutableList.of(container, leaf);

            this.asSetNid = new NodeIdentifier(QName.create(namespace, "as-set").intern());
            this.asSeqNid = new NodeIdentifier(QName.create(namespace, "as-sequence").intern());
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

        NodeIdentifier getAsSet() {
            return this.asSetNid;
        }

        NodeIdentifier getAsSeq() {
            return this.asSeqNid;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(BestPathStateImpl.class);
    private static final Cache<QNameModule, NamespaceSpecificIds> PATH_CACHE = CacheBuilder.newBuilder().weakKeys().weakValues().build();

    private long peerAs = 0L;
    private int asPathLength = 0;

    private final ContainerNode attributes;
    private final NamespaceSpecificIds ids;
    private Long localPref;
    private Long multiExitDisc;
    private BgpOrigin origin;
    private boolean resolved;

    public BestPathStateImpl(final ContainerNode attributes) {
        final NamespaceSpecificIds col;
        try {
            col = PATH_CACHE.get(attributes.getNodeType().getModule(), () -> new NamespaceSpecificIds(attributes.getNodeType()));
        } catch (final ExecutionException e) {
            LOG.error("Error creating namespace-specific attributes collection.", e);
            throw new IllegalStateException("Error creating namespace-specific attributes collection.", e);
        }

        this.attributes = requireNonNull(attributes);
        this.ids = col;
        resolveValues();
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

        final Optional<NormalizedNode<?, ?>> maybeLocalPref = NormalizedNodes.findNode(this.attributes, this.ids.getLocPref());
        if (maybeLocalPref.isPresent()) {
            this.localPref = (Long) maybeLocalPref.get().getValue();
        } else {
            this.localPref = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeMultiExitDisc = NormalizedNodes.findNode(this.attributes, this.ids.getMed());
        if (maybeMultiExitDisc.isPresent()) {
            this.multiExitDisc = (Long) maybeMultiExitDisc.get().getValue();
        } else {
            this.multiExitDisc = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeOrigin = NormalizedNodes.findNode(this.attributes, this.ids.getOrig());
        if (maybeOrigin.isPresent()) {
            this.origin = fromString((String) maybeOrigin.get().getValue());
        } else {
            this.origin = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeSegments = NormalizedNodes.findNode(this.attributes, this.ids.getAsPath());
        if (maybeSegments.isPresent()) {
            final UnkeyedListNode segments = (UnkeyedListNode) maybeSegments.get();
            final List<Segments> segs = extractSegments(segments);
            if (!segs.isEmpty()) {
                this.peerAs = getPeerAs(segs).getValue();
                this.asPathLength = countAsPath(segs);
            }
        }
        this.resolved = true;
    }

    @Override
    public Long getLocalPref() {
        resolveValues();
        return this.localPref;
    }

    @Override
    public Long getMultiExitDisc() {
        resolveValues();
        return this.multiExitDisc;
    }

    @Override
    public BgpOrigin getOrigin() {
        resolveValues();
        return this.origin;
    }

    @Override
    public Long getPeerAs() {
        resolveValues();
        return this.peerAs;
    }

    @Override
    public int getAsPathLength() {
        resolveValues();
        return this.asPathLength;
    }

    private static int countAsPath(final List<Segments> segments) {
        // an AS_SET counts as 1, no matter how many ASs are in the set.
        int count = 0;
        boolean setPresent = false;
        for (final Segments s : segments) {
            if (s.getAsSet() != null && !setPresent) {
                setPresent = true;
                count++;
            } else if (s.getAsSequence() != null) {
                count += s.getAsSequence().size();
            }
        }
        return count;
    }

    private static AsNumber getPeerAs(final List<Segments> segments) {
        if (segments.isEmpty()) {
            return new AsNumber(0L);
        }
        for (final Segments seg : segments) {
            if (seg.getAsSequence() != null && !seg.getAsSequence().isEmpty()) {
                return segments.get(0).getAsSequence().get(0);
            }
        }
        return new AsNumber(0L);
    }

    public List<Segments> extractSegments(final UnkeyedListNode segments) {
        // list segments
        final List<Segments> extracted = new ArrayList<>();
        for (final UnkeyedListEntryNode segment : segments.getValue()) {
            final SegmentsBuilder sb = new SegmentsBuilder();
            // We are expecting that segment contains either as-sequence or as-set, so just one of them will be set, other would be null
            sb.setAsSequence(extractAsList(segment, this.ids.getAsSeq())).setAsSet(extractAsList(segment, this.ids.getAsSet()));
            extracted.add(sb.build());
        }
        return extracted;
    }

    private static List<AsNumber> extractAsList(final UnkeyedListEntryNode segment, final NodeIdentifier nid) {
        final List<AsNumber> ases = new ArrayList<>();
        final Optional<NormalizedNode<?, ?>> maybeAsList = NormalizedNodes.findNode(segment, nid);
        if (maybeAsList.isPresent()) {
            final LeafSetNode<?> list = (LeafSetNode<?>)maybeAsList.get();
            for (final LeafSetEntryNode<?> as : list.getValue())  {
                ases.add(new AsNumber((Long)as.getValue()));
            }
            return ases;
        }
        return null;
    }

    @Override
    public ContainerNode getAttributes() {
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
        result = prime * result + (this.localPref == null ? 0 : this.localPref.hashCode());
        result = prime * result + (this.multiExitDisc == null ? 0 : this.multiExitDisc.hashCode());
        result = prime * result + (this.origin == null ? 0 : this.origin.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BestPathStateImpl)) {
            return false;
        }
        final BestPathStateImpl other = (BestPathStateImpl) obj;
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
