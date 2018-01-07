/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Interface implemented to be extended by RibSupport.
 * This interface exposes methods to access to Add Path information
 * By default we implement non supported Multiple Path therefore
 * 0 Path Id is returned and null PathArgument
 */
interface AddPathRibSupport {
    /**
     * Extract PathId from route change received.
     *
     * @param normalizedNode Path Id Container
     * @return pathId  The path identifier value
     */
    default Long extractPathId(@Nonnull NormalizedNode<?, ?> normalizedNode) {
        return NON_PATH_ID;
    }

    /**
     * Construct a PathArgument to an AddPathRoute.
     *
     * @param pathId  The path identifier
     * @param routeId PathArgument leaf path
     * @return routeId PathArgument + pathId or Null in case Add-path is not supported
     */
    @Nullable
    default NodeIdentifierWithPredicates getRouteIdAddPath(long pathId, @Nonnull PathArgument routeId) {
        return null;
    }

    /**
     * Create a new Path Argument for route Key removing remove Path Id from key.
     * For non extension which doesnt support Multiple Path this step is not required.
     *
     * @param routeKeyPathArgument routeKey Path Argument
     * @return new route Key
     */
    default @Nonnull
    NodeIdentifierWithPredicates createRouteKeyPathArgument(
            @Nonnull NodeIdentifierWithPredicates routeKeyPathArgument) {
        return routeKeyPathArgument;
    }
}
