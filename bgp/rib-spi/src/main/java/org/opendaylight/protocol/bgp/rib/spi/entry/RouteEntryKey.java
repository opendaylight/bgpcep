/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * Route Entry Key containing prefix.
 */
public interface RouteEntryKey<N extends Identifier> {
    /**
     * Returns route containing prefix.
     *
     * @return Route key
     */
    @Nonnull
    N getRouteKey();
}
