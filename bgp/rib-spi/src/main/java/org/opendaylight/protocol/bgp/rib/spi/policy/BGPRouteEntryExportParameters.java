/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi.policy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.PeerRole;

/**
 * Contains Peer destiny information for export route entry.
 */
public interface BGPRouteEntryExportParameters extends BGPRouteEntryImportParameters {

    /**
     * Peer id of Peer route entry destiny.
     *
     * @return peer Id of announcer Peer
     */
    @Nonnull
    PeerId getToPeerId();

    /**
     * Peer id of Peer route entry destiny.
     *
     * @return peer Role of announcer Peer
     */
    @Nonnull
    PeerRole getToPeerRole();

    /**
     * Peer local AS of Peer route entry destiny.
     *
     * @return peer Local AS of destiny Peer
     */
    @Nullable
    AsNumber getToPeerLocalAs();
}
