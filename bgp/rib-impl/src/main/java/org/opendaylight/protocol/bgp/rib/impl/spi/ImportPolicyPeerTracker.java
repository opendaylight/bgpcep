/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.annotations.Beta;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Tracks import policy corresponding to a particular peer.
 */
@Beta
public interface ImportPolicyPeerTracker {

    /**
     * Invoked whenever a peer role changes.
     *
     * @param peerPath Peer's path
     * @param role Peer's new role, null indicates the peer has disappeared.
     */
    void peerRoleChanged(YangInstanceIdentifier peerPath, PeerRole role);

    AbstractImportPolicy policyFor(PeerId peerId);

}
