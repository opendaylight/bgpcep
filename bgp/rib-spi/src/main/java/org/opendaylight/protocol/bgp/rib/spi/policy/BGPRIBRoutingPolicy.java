/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.policy;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

/**
 * Interface for BGP RIB Routing Policy. Apply Import/Export Routing Policy to route attributes.
 */
public interface BGPRIBRoutingPolicy {
    /**
     * Apply import policy to route attributes
     *
     * @param key          Route key
     * @param fromPeerId   peer Id of route announcer
     * @param fromPeerRole peer role of route announcer
     * @param attributes   route attributes
     * @return modified route attributes after apply policies
     */
    @Nonnull
    Optional<ContainerNode> applyImportPolicies(@Nonnull final PathArgument key, @Nonnull final PeerId fromPeerId,
        @Nonnull final PeerRole fromPeerRole, @Nullable ContainerNode attributes);

    /**
     * Apply export policy to route attributes
     *
     * @param key          Route key
     * @param fromPeerId   peer Id of route announcer
     * @param fromPeerRole peer role of route announcer
     * @param toPeer       peer Id of route announced
     * @param toPeerRole   peer role of route announced
     * @param attributes   route attributes
     * @return modified route attributes after apply policies
     */
    @Nonnull
    Optional<ContainerNode> applyExportPolicies(@Nonnull final PathArgument key, @Nonnull final PeerId fromPeerId,
        @Nonnull final PeerRole fromPeerRole, @Nonnull final PeerId toPeer, @Nonnull final PeerRole toPeerRole,
        @Nullable ContainerNode attributes);
}
