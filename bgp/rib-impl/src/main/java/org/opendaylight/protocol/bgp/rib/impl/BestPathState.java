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
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.AListCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.as.path.segment.c.segment.ASetCase;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

@NotThreadSafe
final class BestPathState {
    private static final Collection<PathArgument> AS_PATH = ImmutableList.<PathArgument>of(new NodeIdentifier(AsPath.QNAME), new NodeIdentifier(Segments.QNAME));
    private static final Collection<PathArgument> LOCAL_PREF = ImmutableList.<PathArgument>of(new NodeIdentifier(LocalPref.QNAME), new NodeIdentifier(QName.create(LocalPref.QNAME, "pref")));
    private static final Collection<PathArgument> MED = ImmutableList.<PathArgument>of(new NodeIdentifier(MultiExitDisc.QNAME), new NodeIdentifier(QName.create(MultiExitDisc.QNAME, "med")));
    private static final Collection<PathArgument> ORIGIN = ImmutableList.<PathArgument>of(new NodeIdentifier(Origin.QNAME), new NodeIdentifier(QName.create(Origin.QNAME, "value")));

    private final ContainerNode attributes;
    private Long localPref;
    private Long multiExitDisc;
    private BgpOrigin origin;
    private final Long peerAs = 0L;
    private final int asPathLength = 0;
    private boolean resolved;

    BestPathState(final ContainerNode attributes) {
        this.attributes = Preconditions.checkNotNull(attributes);
    }

    private void resolveValues() {
        if (resolved) {
            return;
        }

        final Optional<NormalizedNode<?, ?>> maybeLocalPref = NormalizedNodes.findNode(attributes, LOCAL_PREF);
        if (maybeLocalPref.isPresent()) {
            localPref = (Long) ((LeafNode<?>)maybeLocalPref.get()).getValue();
        } else {
            localPref = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeMultiExitDisc = NormalizedNodes.findNode(attributes, MED);
        if (maybeMultiExitDisc.isPresent()) {
            multiExitDisc = (Long) ((LeafNode<?>)maybeMultiExitDisc.get()).getValue();
        } else {
            multiExitDisc = null;
        }

        final Optional<NormalizedNode<?, ?>> maybeOrigin = NormalizedNodes.findNode(attributes, ORIGIN);
        if (maybeOrigin.isPresent()) {
            final String originStr = (String) ((LeafNode<?>)maybeOrigin.get()).getValue();
            switch (originStr) {
            case "igp":
                origin = BgpOrigin.Igp;
                break;
            case "egp":
                origin = BgpOrigin.Egp;
                break;
            case "incomplete":
                origin = BgpOrigin.Incomplete;
                break;
            default:
                throw new IllegalArgumentException("Unhandleed origin value " + originStr);
            }
        } else {
            origin = null;
        }

        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeSegments = NormalizedNodes.findNode(attributes, AS_PATH);
        if (maybeSegments.isPresent()) {
            final UnkeyedListNode segments = (UnkeyedListNode) maybeSegments.get();

            if (segments.getSize() != 0) {
                // FIXME: peer AS number

                // FIXME: asPathLength = countAsPath(this.bestState.getAsPath().getSegments());
                boolean haveSegment;
                for (UnkeyedListEntryNode s : segments.getValue()) {

                }
            }
        }

        resolved = true;
    }

    Long getLocalPref() {
        resolveValues();
        return localPref;
    }

    Long getMultiExitDisc() {
        resolveValues();
        return multiExitDisc;
    }

    BgpOrigin getOrigin() {
        resolveValues();
        return origin;
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

}