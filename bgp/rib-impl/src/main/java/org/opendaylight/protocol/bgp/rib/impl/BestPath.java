/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedInteger;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;

/**
 * Class encapsulating various information about the currently-selected
 * best path. Instances should only be created from {@link BestPathSelector}.
 */
final class BestPath {
    private final UnsignedInteger routerId;
    private final PathAttributes attributes;

    BestPath(@Nonnull final UnsignedInteger routerId, @Nonnull final PathAttributes attributes) {
        this.routerId = Preconditions.checkNotNull(routerId);
        this.attributes = Preconditions.checkNotNull(attributes);
    }

    UnsignedInteger getRouterId() {
        return routerId;
    }

    PathAttributes getAttributes() {
        return attributes;
    }
}
