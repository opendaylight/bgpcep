/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.rib.spi.Peer;

/**
 * RouteEntryInfo wrapper contains all related information from new best path.
 */
@NonNullByDefault
public interface RouteEntryInfo {
    /**
     * peer Id where best path will be advertized.
     *
     * @return PeerId
     */
    Peer getToPeer();

    /**
     * Returns route containing prefix.
     *
     * @return Route key
     */
    String getRouteKey();
}
