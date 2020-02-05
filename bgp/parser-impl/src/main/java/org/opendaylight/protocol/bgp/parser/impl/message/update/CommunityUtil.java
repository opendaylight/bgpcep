/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Community;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Object representation of a RFC1997 Community tag. Communities are a way for the advertising entity to attach semantic
 * meaning/policy to advertised objects.
 */
public final class CommunityUtil {
    /**
     * NO_EXPORT community. All routes received carrying a communities attribute containing this value MUST NOT be
     * advertised outside a BGP confederation boundary (a stand-alone autonomous system that is not part of a
     * confederation should be considered a confederation itself).
     */
    public static final Community NO_EXPORT
            = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF01);
    /**
     * NO_ADVERTISE community. All routes received carrying a communities attribute containing this value MUST NOT be
     * advertised to other BGP peers.
     */
    public static final Community NO_ADVERTISE
            = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF02);
    /**
     * NO_EXPORT_SUBCONFED community. All routes received carrying a communities attribute containing this value MUST
     * NOT be advertised to external BGP peers (this includes peers in other members autonomous systems inside a BGP
     * confederation).
     */
    public static final Community NO_EXPORT_SUBCONFED
            = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF03);

    /**
     * LLGR_STALE community can be used to mark stale routes retained for a longer period of time.
     * Such long-lived stale routes are to be handled according to the procedures specified in
     * https://tools.ietf.org/html/draft-uttaro-idr-bgp-persistence-04#section-4.
     */
    public static final Community LLGR_STALE
            = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0x0006);

    /**
     * NO_LLGR community can be used to mark routes which a BGP speaker does not want treated according
     * to procedures, as detailed in https://tools.ietf.org/html/draft-uttaro-idr-bgp-persistence-04#section-4.
     */
    public static final Community NO_LLGR
            = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0x0007);

    private final ReferenceCache refCache;

    public CommunityUtil(final ReferenceCache refCache) {
        this.refCache = requireNonNull(refCache);
    }

    /**
     * Creates a new Community given AS number value and semantics using generated CommunitiesBuilder.
     *
     * @param asn long
     * @param semantics int
     * @return new Community
     */
    public Community create(final long asn, final int semantics) {
        return create(this.refCache, asn, semantics);
    }

    /**
     * Creates a new Community given AS number value and semantics using generated CommunitiesBuilder.
     *
     * @param refCache reference cache to use
     * @param asn long
     * @param semantics int
     * @return new Community
     */
    // FIXME: consider using Uint32 for asn
    // FIXME: consider using Uint16 for semantics
    public static Community create(final ReferenceCache refCache, final long asn, final int semantics) {
        final CommunitiesBuilder builder = new CommunitiesBuilder();
        builder.setAsNumber(refCache.getSharedReference(new AsNumber(Uint32.valueOf(asn))));
        builder.setSemantics(refCache.getSharedReference(Uint16.valueOf(semantics)));
        return refCache.getSharedReference(builder.build());
    }

    /**
     * Creates a Community from its String representation.
     *
     * @param string String representation of a community
     * @return new Community
     */
    public Community valueOf(final String string) {
        return valueOf(this.refCache, string);
    }

    /**
     * Creates a Community from its String representation.
     *
     * @param refCache reference cache to use
     * @param string String representation of a community
     * @return new Community
     */
    public static Community valueOf(final ReferenceCache refCache, final String string) {
        final String[] parts = string.split(":", 2);
        final CommunitiesBuilder builder = new CommunitiesBuilder();
        builder.setAsNumber(refCache.getSharedReference(new AsNumber(Uint32.valueOf(parts[0]))));
        builder.setSemantics(refCache.getSharedReference(Uint16.valueOf(parts[1])));
        return refCache.getSharedReference(builder.build());
    }
}
