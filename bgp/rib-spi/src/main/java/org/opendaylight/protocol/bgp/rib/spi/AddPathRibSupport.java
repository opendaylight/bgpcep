/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Interface implemented to be extended by RibSupport.
 * This interface exposes methods to access to Add Path information
 */
public interface AddPathRibSupport {
    /**
     * Extract PathId from route change received
     *
     * @param normalizedNode
     * @return pathId  The path identifier
     */
    long extractPathId(NormalizedNode<?, ?> normalizedNode);

    /**
     * Construct a PathArgument to an AddPathRoute
     *
     * @param pathId  The path identifier
     * @param routeId PathArgument leaf path
     * @return routeId PathArgument + pathId
     */
    @Nonnull PathArgument getRouteIdAddPath(long pathId, PathArgument routeId);
}
