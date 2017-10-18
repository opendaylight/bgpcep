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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;

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
    public static final Community NO_EXPORT = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF01);
    /**
     * NO_ADVERTISE community. All routes received carrying a communities attribute containing this value MUST NOT be
     * advertised to other BGP peers.
     */
    public static final Community NO_ADVERTISE = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF02);
    /**
     * NO_EXPORT_SUBCONFED community. All routes received carrying a communities attribute containing this value MUST
     * NOT be advertised to external BGP peers (this includes peers in other members autonomous systems inside a BGP
     * confederation).
     */
    public static final Community NO_EXPORT_SUBCONFED = CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF03);

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
     * Creates a Community from its String representation.
     *
     * @param string String representation of a community
     * @return new Community
     */
    public Community valueOf(final String string) {
        return valueOf(this.refCache, string);
    }

    /**
     * Creates a new Community given AS number value and semantics using generated CommunitiesBuilder.
     *
     * @param refCache reference cache to use
     * @param asn long
     * @param semantics int
     * @return new Community
     */
    public static Community create(final ReferenceCache refCache, final long asn, final int semantics) {
        final CommunitiesBuilder builder = new CommunitiesBuilder();
        builder.setAsNumber(refCache.getSharedReference(new AsNumber(asn)));
        builder.setSemantics(refCache.getSharedReference(semantics));
        return refCache.getSharedReference(builder.build());
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
        builder.setAsNumber(refCache.getSharedReference(new AsNumber(Long.valueOf(parts[0]))));
        builder.setSemantics(refCache.getSharedReference(Integer.valueOf(parts[1])));
        return refCache.getSharedReference(builder.build());
    }
}
