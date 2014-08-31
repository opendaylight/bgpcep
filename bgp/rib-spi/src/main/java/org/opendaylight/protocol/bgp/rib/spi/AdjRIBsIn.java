/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;

public interface AdjRIBsIn<K, V extends Route> {

    /**
     * Adds routes to this adjacency rib.
     * @param trans data-store transaction
     * @param peer advertising peer
     * @param nlri routes
     * @param attributes route attributes
     */
    void addRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpReachNlri nlri, final PathAttributes attributes);

    /**
     * Removes routes from this adjacency rib.
     * @param trans data-store transaction
     * @param peer advertising peer
     * @param nlri routes
     */
    void removeRoutes(final AdjRIBsTransaction trans, final Peer peer, final MpUnreachNlri nlri);

    /**
     * Clears adjacency rib tables.
     * @param trans data-store transaction
     * @param peer advertising peer
     */
    void clear(final AdjRIBsTransaction trans, final Peer peer);

    /**
     * Marks true or false the state of this adjacency rib.
     * @param trans data-store transaction
     * @param peer advertising peer
     */
    void markUptodate(final AdjRIBsTransaction trans, final Peer peer);

    /**
     * Transform an advertised data object into the corresponding NLRI in MP_REACH attribute.
     * @param builder MP_REACH attribute builder
     * @param data Data object
     */
    void addAdvertisement(final MpReachNlriBuilder builder, final V data);

    /**
     * Creates end-of-rib message for this adjacency rib.
     * @return BGP Update message
     */
    Update endOfRib();
}
