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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;

public final class BestPathStateImpl implements BestPathState {
    private final Attributes attributes;
    private long peerAs = 0L;
    private int asPathLength = 0;
    private Long localPref;
    private Long multiExitDisc;
    private BgpOrigin origin;
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
            } else if (s.getAsSequence() != null) {
                count += s.getAsSequence().size();
            }
        }
        return count;
    }

    private void resolveValues() {
        if (this.resolved) {
            return;
        }

        if (this.attributes.getLocalPref() != null) {
            this.localPref = this.attributes.getLocalPref().getPref();
        } else {
            this.localPref = null;
        }

        if (this.attributes.getMultiExitDisc() != null) {
            this.multiExitDisc = this.attributes.getMultiExitDisc().getMed();
        } else {
            this.multiExitDisc = null;
        }

        if (this.attributes.getOrigin() != null) {
            this.origin = this.attributes.getOrigin().getValue();
        } else {
            this.origin = null;
        }
        if (this.attributes.getAsPath() != null) {
            final List<Segments> segs = this.attributes.getAsPath().getSegments();
            if (!segs.isEmpty()) {
                this.peerAs = BesthPathStateUtil.getPeerAs(segs);
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
