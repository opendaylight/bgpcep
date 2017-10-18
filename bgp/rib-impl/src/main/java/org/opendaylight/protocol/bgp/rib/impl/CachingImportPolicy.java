/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.protocol.bgp.rib.impl.spi.AbstractImportPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Attributes;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * A caching decorator for {@link AbstractImportPolicy}. Performs caching of effective
 * attributes using an identity-and-hashCode-based map for fast lookup and reuse of resulting
 * objects.
 */
@NotThreadSafe
final class CachingImportPolicy extends AbstractImportPolicy {

    // A dummy ContainerNode, stored in the cache to indicate null effective attributes
    private static final ContainerNode MASKED_NULL = ImmutableNodes.containerNode(Attributes.QNAME);

    // We maintain a weak cache of returned effective attributes, so we end up reusing
    // the same instance when asked. We set concurrency level to 1, as we do not expect
    // the cache to be accessed from multiple threads. That may need to be changed
    // if we end up sharing the cache across peers.
    private final ConcurrentMap<ContainerNode, ContainerNode> cache =
            new MapMaker().concurrencyLevel(1).weakKeys().weakValues().makeMap();

    /*
     * The cache itself is weak, which means we end up with identity hash/comparisons.
     * That is good, but we want the cache to be effective even when equivalent attributes
     * are presented. For that purpose we maintain a weak interner, which will allow us
     * to map attributes to a canonical object without preventing garbage collection.
     */
    private final Interner<ContainerNode> interner = Interners.newWeakInterner();

    private final AbstractImportPolicy delegate;

    CachingImportPolicy(final AbstractImportPolicy delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Nonnull private static ContainerNode maskNull(@Nullable final ContainerNode unmasked) {
        return unmasked == null ? MASKED_NULL : unmasked;
    }

    @Nullable private static ContainerNode unmaskNull(@Nonnull final ContainerNode masked) {
        return MASKED_NULL.equals(masked) ? null : masked;
    }

    @Override
    public ContainerNode effectiveAttributes(final ContainerNode attributes) {
        ContainerNode ret = this.cache.get(attributes);
        if (ret != null) {
            return unmaskNull(ret);
        }

        /*
         * The cache returned empty. The reason for that may be that the attributes
         * passed in are not identical to the ones forming the cache's key. Intern
         * the passed attributes, which will result in a canonical reference.
         *
         * If the returned reference is different, attempt to look up in the cache
         * again. If the reference is the same, we have just populated the interner
         * and thus are on the path to create a new cache entry.
         */
        final ContainerNode interned = this.interner.intern(attributes);
        if (!interned.equals(attributes)) {
            final ContainerNode retry = this.cache.get(interned);
            if (retry != null) {
                return unmaskNull(retry);
            }
        }

        final ContainerNode effective = this.delegate.effectiveAttributes(interned);

        /*
         * Populate the cache. Note that this may have raced with another thread,
         * in which case we want to reuse the previous entry without replacing it.
         * Check the result of conditional put and return it unmasked if it happens
         * to be non-null. That will throw away the attributes we just created,
         * but that's fine, as they have not leaked to heap yet and will be GC'd
         * quickly.
         */
        final ContainerNode existing = this.cache.putIfAbsent(interned, maskNull(effective));
        return existing != null ? unmaskNull(existing) : effective;

    }
}
