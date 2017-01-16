/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.spi.policy.RouteAttributesContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class RouteAttributesContainerImpl extends AbstractRouteAttributesContainer implements RouteAttributesContainer {
    private final PeerId fromPeerId;
    private final PeerRole fromPeerRole;
    private final PeerRole toPeerRole;
    private final ContainerNode attributes;

    public RouteAttributesContainerImpl(@Nonnull final PathArgument key, @Nonnull final PeerId fromPeerId,
        @Nonnull final PeerRole fromPeerRole, @Nonnull final PeerId toPeerId, @Nonnull final PeerRole toPeerRole,
        @Nullable final PathArgument routeIdPA, @Nonnull final ContainerNode attributes) {
        super(key, toPeerId, routeIdPA);
        this.fromPeerId = fromPeerId;
        this.fromPeerRole = fromPeerRole;
        this.toPeerRole = toPeerRole;
        this.attributes = attributes;
    }

    @Override
    public PeerId getFromPeerId() {
        return this.fromPeerId;
    }

    @Override
    public ContainerNode getAttributes() {
        return this.attributes;
    }

    @Override
    public PeerRole getFromPeerRole() {
        return this.fromPeerRole;
    }

    @Override
    public PeerRole getToPeerRole() {
        return this.toPeerRole;
    }
}
