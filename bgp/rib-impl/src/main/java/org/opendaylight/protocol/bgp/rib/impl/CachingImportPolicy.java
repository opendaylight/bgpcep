/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import java.util.IdentityHashMap;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * A caching decorator for {@link AbstractImportPolicy}. Performs caching of effective
 * attributes using an {@link IdentityHashMap} for fast lookup and reuse of resulting
 * objects.
 */
@NotThreadSafe
final class CachingImportPolicy extends AbstractImportPolicy {
    private final IdentityHashMap<ContainerNode, ContainerNode> cache = new IdentityHashMap<>();
    private final AbstractImportPolicy delegate;

    CachingImportPolicy(final AbstractImportPolicy delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    ContainerNode effectiveAttributes(final ContainerNode attributes) {
        ContainerNode ret = cache.get(attributes);
        if (ret == null) {
            ret = delegate.effectiveAttributes(attributes);
            if (ret != null) {
                cache.put(attributes, ret);
            }
        }

        return ret;
    }
}
