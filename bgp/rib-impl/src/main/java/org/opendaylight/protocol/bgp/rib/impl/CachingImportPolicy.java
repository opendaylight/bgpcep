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
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.protocol.bgp.rib.impl.spi.ImportPolicy;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * A caching decorator for {@link ImportPolicy}. Performs caching of effective
 * attributes using an {@link IdentityHashMap} for fast lookup and reuse of resulting
 * objects.
 */
@NotThreadSafe
final class CachingImportPolicy implements ImportPolicy {
    private final Map<ContainerNode, ContainerNode> cache = new IdentityHashMap<>();
    private final ImportPolicy delegate;

    CachingImportPolicy(final ImportPolicy delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public ContainerNode effectiveAttributes(final ContainerNode attributes) {
        ContainerNode ret = this.cache.get(attributes);
        if (ret == null) {
            ret = this.delegate.effectiveAttributes(attributes);
            if (ret != null) {
                this.cache.put(attributes, ret);
            }
        }

        return ret;
    }
}
