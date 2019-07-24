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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.mode.BesthPathStateUtil;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class BestPathStateImpl implements BestPathState {
    private static final class NamespaceSpecificIds {
        final List<PathArgument> asPath;
        final List<PathArgument> locPref;
        final List<PathArgument> med;
        final List<PathArgument> orig;
        final NodeIdentifier asSetNid;
        final NodeIdentifier asSeqNid;
        final NodeIdentifier communities;
        final NodeIdentifier asNumber;
        final NodeIdentifier semantics;

        NamespaceSpecificIds(final @NonNull QNameModule namespace) {
            this.asPath = List.of(NodeIdentifier.create(AsPath.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "segments").intern()));
            this.locPref = List.of(NodeIdentifier.create(LocalPref.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "pref").intern()));
            this.med = List.of(NodeIdentifier.create(MultiExitDisc.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "med").intern()));
            this.orig = List.of(NodeIdentifier.create(Origin.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "value").intern()));
            this.asSetNid = NodeIdentifier.create(QName.create(namespace, "as-set").intern());
            this.asSeqNid = NodeIdentifier.create(QName.create(namespace, "as-sequence").intern());
            this.communities = NodeIdentifier.create(Communities.QNAME.bindTo(namespace).intern());
            this.asNumber = NodeIdentifier.create(QName.create(namespace, "as-number").intern());
            this.semantics = NodeIdentifier.create(QName.create(namespace, "semantics").intern());
        }
    }

    private static final LoadingCache<QNameModule, NamespaceSpecificIds> PATH_CACHE =
        CacheBuilder.newBuilder().weakKeys().weakValues().build(new CacheLoader<>() {
            @Override
            public NamespaceSpecificIds load(final QNameModule key) {
                return new NamespaceSpecificIds(key);
            }
        });

    private static final Uint32 LLGR_STALE_AS_NUMBER = CommunityUtil.LLGR_STALE.getAsNumber().getValue().intern();
    private static final Uint16 LLGR_STALE_SEMANTICS = CommunityUtil.LLGR_STALE.getSemantics().intern();

    private final ContainerNode attributes;

    private long peerAs = 0L;
    private int asPathLength = 0;
    private Uint32 localPref;
    private long multiExitDisc;
    private BgpOrigin origin;
    private boolean depreferenced;
    private boolean resolved;

    public BestPathStateImpl(final ContainerNode attributes) {
        this.attributes = requireNonNull(attributes);
        resolveValues();
    }

    private void resolveValues() {
        if (resolved) {
            return;
        }

        final NamespaceSpecificIds ids = PATH_CACHE.getUnchecked(attributes.getNodeType().getModule());
        localPref = (Uint32) NormalizedNodes.findNode(attributes, ids.locPref)
            .map(NormalizedNode::getValue)
            .orElse(null);

        final Optional<NormalizedNode<?, ?>> maybeMultiExitDisc = NormalizedNodes.findNode(attributes, ids.med);
        if (maybeMultiExitDisc.isPresent()) {
            multiExitDisc = ((Uint32) maybeMultiExitDisc.get().getValue()).toJava();
        } else {
            multiExitDisc = 0L;
        }

        final Optional<NormalizedNode<?, ?>> maybeOrigin = NormalizedNodes.findNode(attributes, ids.orig);
        if (maybeOrigin.isPresent()) {
            final String originStr = (String) maybeOrigin.get().getValue();
            origin = BgpOrigin.forName(originStr)
                .orElseThrow(() -> new IllegalArgumentException("Unhandled origin value " + originStr));
        } else {
            origin = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeSegments = NormalizedNodes.findNode(attributes, ids.asPath);
        if (maybeSegments.isPresent()) {
            final UnkeyedListNode segments = (UnkeyedListNode) maybeSegments.get();
            final List<Segments> segs = extractSegments(segments, ids);
            if (!segs.isEmpty()) {
                this.peerAs = BesthPathStateUtil.getPeerAs(segs);
                this.asPathLength = countAsPath(segs);
            }
        }

        final Optional<NormalizedNode<?, ?>> maybeCommunities = NormalizedNodes.findNode(attributes, ids.communities);
        if (maybeCommunities.isPresent()) {
            depreferenced = ((UnkeyedListNode) maybeCommunities.orElseThrow()).getValue().stream()
                .anyMatch(community -> isStale(ids, community));
        } else {
            depreferenced = false;
        }

        this.resolved = true;
    }

    private static boolean isStale(final NamespaceSpecificIds ids, final UnkeyedListEntryNode community) {
        return LLGR_STALE_AS_NUMBER.equals(community.getChild(ids.asNumber).orElseThrow().getValue())
            && LLGR_STALE_SEMANTICS.equals(community.getChild(ids.semantics).orElseThrow().getValue());
    }

    @Override
    public Uint32 getLocalPref() {
        resolveValues();
        return this.localPref;
    }

    @Override
    public long getMultiExitDisc() {
        resolveValues();
        return this.multiExitDisc;
    }

    @Override
    public BgpOrigin getOrigin() {
        resolveValues();
        return this.origin;
    }

    @Override
    public long getPeerAs() {
        resolveValues();
        return this.peerAs;
    }

    @Override
    public int getAsPathLength() {
        resolveValues();
        return this.asPathLength;
    }

    @Override
    public ContainerNode getAttributes() {
        return this.attributes;
    }

    @Override
    public boolean isDepreferenced() {
        resolveValues();
        return depreferenced;
    }

    private ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("attributes", this.attributes);
        toStringHelper.add("localPref", this.localPref);
        toStringHelper.add("multiExitDisc", this.multiExitDisc);
        toStringHelper.add("origin", this.origin);
        toStringHelper.add("resolved", this.resolved);
        toStringHelper.add("depreferenced", this.depreferenced);
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
        result = prime * result + Long.hashCode(multiExitDisc);
        result = prime * result + (this.origin == null ? 0 : this.origin.hashCode());
        result = prime * result + Boolean.hashCode(depreferenced);
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
        if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (!Objects.equals(this.localPref, other.localPref)) {
            return false;
        }
        if (multiExitDisc != other.multiExitDisc) {
            return false;
        }
        if (origin != other.origin) {
            return false;
        }
        return depreferenced == other.depreferenced;
    }

    private List<Segments> extractSegments(final UnkeyedListNode segments, final NamespaceSpecificIds ids) {
        // list segments
        final List<Segments> extracted = new ArrayList<>();
        for (final UnkeyedListEntryNode segment : segments.getValue()) {
            final SegmentsBuilder sb = new SegmentsBuilder();
            // We are expecting that segment contains either as-sequence or as-set,
            // so just one of them will be set, other would be null
            sb.setAsSequence(extractAsList(segment, ids.asSeqNid))
                    .setAsSet(extractAsList(segment, ids.asSetNid));
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
                ases.add(new AsNumber((Uint32) as.getValue()));
            }
            return ases;
        }
        return null;
    }

    private static int countAsPath(final List<Segments> segments) {
        // an AS_SET counts as 1, no matter how many ASs are in the set.
        int count = 0;
        boolean setPresent = false;
        for (final Segments s : segments) {
            if (s.getAsSet() != null && !setPresent) {
                setPresent = true;
                count++;
            } else {
                final List<AsNumber> seq = s.getAsSequence();
                if (seq != null) {
                    count += seq.size();
                }
            }
        }
        return count;
    }
}
