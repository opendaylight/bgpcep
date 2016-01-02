/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.primitives.UnsignedInteger;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface RouteEntry {

    int addRoute(final UnsignedInteger routerId, final YangInstanceIdentifier.NodeIdentifier attributesIdentifier, final NormalizedNode<?, ?> data);

    /**
     * Indicates whether best has changed
     *
     * @param localAs
     * @return
     */
    boolean selectBest(final long localAs);

    /**
     * Indicates whether this was the last route
     *
     * @param offset
     * @return
     */
    boolean removeRoute(final int offset);

    ContainerNode attributes();

    boolean removeRoute(final UnsignedInteger routerId);

    MapEntryNode createValue(YangInstanceIdentifier.PathArgument routeId);
}