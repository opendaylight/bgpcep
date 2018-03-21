/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Loads Peer GROUP configuration from CONFIG DS.
 */
public interface PeerGroupConfigLoader {
    /**
     * Loads Peer GROUP configuration from CONFIG DS.
     *
     * @param instanceIdentifier Protocol BGP Instance identifier.
     * @param neighbor           peer Group name.
     * @return peer group or null.
     */
    @Nullable
    PeerGroup getPeerGroup(@Nonnull InstanceIdentifier<Bgp> instanceIdentifier, @Nonnull String neighbor);
}
