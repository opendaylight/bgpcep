/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * A caching decorator for {@link AbstractImportPolicy}. Performs caching of
 * effective attributes using a {@link Cache} for fast lookup and reuse of
 * resulting objects.
 */
@NotThreadSafe
final class CachingImportPolicy extends AbstractImportPolicy {
    // Restrict cache size to maximum 1000 entries,
    // set to expire after 10 minutes of inactivity
    private final Cache<ContainerNode, ContainerNode> cache = CacheBuilder.newBuilder().maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();
    private final AbstractImportPolicy delegate;

    CachingImportPolicy(final AbstractImportPolicy delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    ContainerNode effectiveAttributes(final ContainerNode attributes) {
        ContainerNode ret = this.cache.getIfPresent(attributes);
        if (ret == null) {
            ret = this.delegate.effectiveAttributes(attributes);
            if (ret != null) {
                this.cache.put(attributes, ret);
            }
        }

        return ret;
    }
}
