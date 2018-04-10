/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.api;

import javax.annotation.Nonnull;

public interface PathSelectionMode extends AutoCloseable {
    /**
     * Create a RouteEntry.
     *
     * @param isComplex true if is complex
     * @return ComplexRouteEntry if is complex otherwise a SimpleRouteEntry
     * @deprecated All routes are complex.
     */
    @Deprecated
    default @Nonnull RouteEntry createRouteEntry(boolean isComplex) {
        return createRouteEntry();
    }

    /**
     * Create a RouteEntry.
     *
     * @return ComplexRouteEntry if is complex otherwise a SimpleRouteEntry
     */
    @Nonnull
    RouteEntry createRouteEntry();
}
