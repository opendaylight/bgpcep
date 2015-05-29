/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class BestPathState {

    private static final Logger LOG = LoggerFactory.getLogger(BestPathState.class);

    private static final class AttributesCollection {
        private final Collection<PathArgument> asPath;
        private final Collection<PathArgument> locPref;
        private final Collection<PathArgument> med;
        private final Collection<PathArgument> orig;

        public AttributesCollection(final QName namespace) {
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

        public Collection<PathArgument> getAsPath() {
            return this.asPath;
        }

        public Collection<PathArgument> getLocPref() {
            return this.locPref;
        }

        public Collection<PathArgument> getMed() {
            return this.med;
        }

        public Collection<PathArgument> getOrig() {
            return this.orig;
        }
    }

    private static Long peerAs = 0L;
    private static int asPathLength = 0;

    private final ContainerNode attributes;
    private final AttributesCollection collection;
    private Long localPref;
    private Long multiExitDisc;
    private BgpOrigin origin;
    private boolean resolved;

    private static final Cache<QNameModule, AttributesCollection> attrs = CacheBuilder.newBuilder().weakValues().build();

    BestPathState(final ContainerNode attributes) {
        this.attributes = Preconditions.checkNotNull(attributes);
        AttributesCollection col = null;
        try {
            col = attrs.get(attributes.getNodeType().getModule(), new Callable<AttributesCollection>() {
                @Override
                public AttributesCollection call() {
                    return new AttributesCollection(attributes.getNodeType());
                }
            });
        } catch (final ExecutionException e) {
            LOG.error("Error creating namespace-specific attributes collection.", e);
            throw new IllegalStateException(e);
        }
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
