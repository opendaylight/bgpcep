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
import java.util.List;
import org.opendaylight.protocol.bgp.mode.BesthPathStateUtil;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunityUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class BestPathStateImpl implements BestPathState {
    private final Attributes attributes;
    private long peerAs = 0L;
    private int asPathLength = 0;
    private Uint32 localPref;
    private long multiExitDisc;
    private BgpOrigin origin;
    private boolean depreferenced;
    private boolean resolved;

    public BestPathStateImpl(final Attributes attributes) {
        this.attributes = requireNonNull(attributes);
        resolveValues();
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

    private void resolveValues() {
        if (resolved) {
            return;
        }

        final LocalPref attrLocalPref = attributes.getLocalPref();
        localPref = attrLocalPref != null ? attrLocalPref.getPref() : null;

        final MultiExitDisc attrMed = attributes.getMultiExitDisc();
        if (attrMed != null) {
            final Uint32 med = attrMed.getMed();
            multiExitDisc = med == null ? 0L : med.toJava();
        } else {
            multiExitDisc = 0L;
        }

        final Origin attrOrigin = attributes.getOrigin();
        origin = attrOrigin != null ? attrOrigin.getValue() : null;

        final AsPath attrAsPath = attributes.getAsPath();
        if (attrAsPath != null) {
            final List<Segments> segs = attrAsPath.getSegments();
            if (segs != null && !segs.isEmpty()) {
                this.peerAs = BesthPathStateUtil.getPeerAs(segs);
                this.asPathLength = countAsPath(segs);
            }
        }

        final List<Communities> attrCommunities = attributes.getCommunities();
        depreferenced = attrCommunities != null && attrCommunities.contains(CommunityUtil.LLGR_STALE);

        this.resolved = true;
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
    public Attributes getAttributes() {
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
        if (this.multiExitDisc != other.multiExitDisc) {
            return false;
        }
        if (this.origin != other.origin) {
            return false;
        }
        return depreferenced == other.depreferenced;
    }
}
