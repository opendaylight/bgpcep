/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Marker interface identifying a BGP peer.
 */
public interface Peer extends PeerTrackerInformation {
    /**
     * Return peer's symbolic name.
     *
     * @return symbolic name.
     */
    @Nonnull
    String getName();

    /**
     * Return the peer's BGP identifier as raw byte array.
     *
     * @return byte[] raw identifier
     */
    byte[] getRawIdentifier();

    /**
     * Close Peers and performs asynchronously DS clean up.
     *
     * @return future
     */
    @Nonnull
    ListenableFuture<?> close();

    void update(InstanceIdentifier ribOutTarget, Route route, Attributes attributes);

    FluentFuture<? extends CommitInfo> delete(InstanceIdentifier ribOutTarget);
}
