/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class BaseBestPathState implements BestPathState {
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
            asPath = List.of(NodeIdentifier.create(AsPath.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "segments").intern()));
            locPref = List.of(NodeIdentifier.create(LocalPref.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "pref").intern()));
            med = List.of(NodeIdentifier.create(MultiExitDisc.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "med").intern()));
            orig = List.of(NodeIdentifier.create(Origin.QNAME.bindTo(namespace).intern()),
                NodeIdentifier.create(QName.create(namespace, "value").intern()));
            asSetNid = NodeIdentifier.create(QName.create(namespace, "as-set").intern());
            asSeqNid = NodeIdentifier.create(QName.create(namespace, "as-sequence").intern());
            communities = NodeIdentifier.create(Communities.QNAME.bindTo(namespace).intern());
            asNumber = NodeIdentifier.create(QName.create(namespace, "as-number").intern());
            semantics = NodeIdentifier.create(QName.create(namespace, "semantics").intern());
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

    public BaseBestPathState(final ContainerNode attributes) {
        this.attributes = requireNonNull(attributes);
        resolveValues();
    }

    private void resolveValues() {
        if (resolved) {
            return;
        }

        final var ids = PATH_CACHE.getUnchecked(attributes.name().getNodeType().getModule());
        localPref = (Uint32) NormalizedNodes.findNode(attributes, ids.locPref)
            .map(NormalizedNode::body)
            .orElse(null);

        final var maybeMultiExitDisc = NormalizedNodes.findNode(attributes, ids.med);
        if (maybeMultiExitDisc.isPresent()) {
            multiExitDisc = ((Uint32) maybeMultiExitDisc.orElseThrow().body()).toJava();
        } else {
            multiExitDisc = 0L;
        }

        final var maybeOrigin = NormalizedNodes.findNode(attributes, ids.orig);
        if (maybeOrigin.isPresent()) {
            final var originStr = (String) maybeOrigin.orElseThrow().body();
            origin = BgpOrigin.forName(originStr);
            if (origin == null) {
                throw new IllegalArgumentException("Unhandled origin value " + originStr);
            }
        } else {
            origin = null;
        }

        final var maybeSegments = NormalizedNodes.findNode(attributes, ids.asPath);
        if (maybeSegments.isPresent()) {
            final var segmentsNode = (UnkeyedListNode) maybeSegments.orElseThrow();
            final var segments = extractSegments(segmentsNode, ids);
            if (!segments.isEmpty()) {
                peerAs = BesthPathStateUtil.getPeerAs(segments);
                asPathLength = countAsPath(segments);
            }
        }

        final var maybeCommunities = NormalizedNodes.findNode(attributes, ids.communities);
        if (maybeCommunities.isPresent()) {
            depreferenced = ((UnkeyedListNode) maybeCommunities.orElseThrow()).body().stream()
                .anyMatch(community -> isStale(ids, community));
        } else {
            depreferenced = false;
        }

        resolved = true;
    }

    private static boolean isStale(final NamespaceSpecificIds ids, final UnkeyedListEntryNode community) {
        return LLGR_STALE_AS_NUMBER.equals(community.getChildByArg(ids.asNumber).body())
            && LLGR_STALE_SEMANTICS.equals(community.getChildByArg(ids.semantics).body());
    }

    @Override
    public Uint32 getLocalPref() {
        resolveValues();
        return localPref;
    }

    @Override
    public long getMultiExitDisc() {
        resolveValues();
        return multiExitDisc;
    }

    @Override
    public BgpOrigin getOrigin() {
        resolveValues();
        return origin;
    }

    @Override
    public long getPeerAs() {
        resolveValues();
        return peerAs;
    }

    @Override
    public int getAsPathLength() {
        resolveValues();
        return asPathLength;
    }

    @Override
    public ContainerNode getAttributes() {
        return attributes;
    }

    @Override
    public boolean isDepreferenced() {
        resolveValues();
        return depreferenced;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = attributes.hashCode();
        result = prime * result + (localPref == null ? 0 : localPref.hashCode());
        result = prime * result + Long.hashCode(multiExitDisc);
        result = prime * result + (origin == null ? 0 : origin.hashCode());
        return prime * result + Boolean.hashCode(depreferenced);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof BaseBestPathState other
            && attributes.equals(other.attributes) && Objects.equals(localPref, other.localPref)
            && multiExitDisc == other.multiExitDisc && origin == other.origin && depreferenced == other.depreferenced;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("attributes", attributes)
            .add("localPref", localPref)
            .add("multiExitDisc", multiExitDisc)
            .add("origin", origin)
            .add("resolved", resolved)
            .add("depreferenced", depreferenced)
            .toString();
    }

    private static List<Segments> extractSegments(final UnkeyedListNode segments, final NamespaceSpecificIds ids) {
        // list segments
        final var extracted = new ArrayList<Segments>();
        for (var segment : segments.body()) {
            final var sb = new SegmentsBuilder();
            // We are expecting that segment contains either as-sequence or as-set,
            // so just one of them will be set, other would be null
            sb.setAsSequence(extractAsList(new ArrayList<>(), segment, ids.asSeqNid))
                    .setAsSet(extractAsList(new LinkedHashSet<>(), segment, ids.asSetNid));
            extracted.add(sb.build());
        }
        return extracted;
    }

    private static <T extends Collection<AsNumber>> T extractAsList(final T ases,
            final UnkeyedListEntryNode segment, final NodeIdentifier nid) {
        final var maybeAsList = NormalizedNodes.findNode(segment, nid);
        if (maybeAsList.isPresent()) {
            final var list = (LeafSetNode<?>) maybeAsList.orElseThrow();
            for (var as : list.body())  {
                ases.add(new AsNumber((Uint32) as.body()));
            }
            return ases;
        }
        return null;
    }

    private static int countAsPath(final List<Segments> segments) {
        // an AS_SET counts as 1, no matter how many ASs are in the set.
        int count = 0;
        boolean setPresent = false;
        for (var segment : segments) {
            if (segment.getAsSet() != null && !setPresent) {
                setPresent = true;
                count++;
            } else {
                final var seq = segment.getAsSequence();
                if (seq != null) {
                    count += seq.size();
                }
            }
        }
        return count;
    }
}
