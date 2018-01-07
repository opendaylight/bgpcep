/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.entry;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * RouteEntryInfo wrapper contains all related information from new best path.
 */
public interface RouteEntryInfo extends RouteEntryKey {
    /**
     * peer Id where best path will be advertized.
     *
     * @return PeerId
     */
    @Nonnull
    PeerId getToPeerId();

    /**
     * Peer path of peer to which best path will be advertized.
     *
     * @return Root Path
     */
    @Nonnull
    YangInstanceIdentifier getRootPath();
}
