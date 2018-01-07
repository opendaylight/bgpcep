/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.entry.RouteEntryInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public final class RouteEntryInfoImpl implements RouteEntryInfo {
    private final PeerId peerId;
    private final NodeIdentifierWithPredicates key;
    private final YangInstanceIdentifier rootPath;

    public RouteEntryInfoImpl(final PeerId peerId, final NodeIdentifierWithPredicates key,
            final YangInstanceIdentifier rootPath) {
        this.peerId = requireNonNull(peerId);
        this.key = requireNonNull(key);
        this.rootPath = requireNonNull(rootPath);
    }

    @Nonnull
    @Override
    public PeerId getToPeerId() {
        return this.peerId;
    }

    @Nonnull
    @Override
    public NodeIdentifierWithPredicates getRouteId() {
        return this.key;
    }

    @Nonnull
    @Override
    public YangInstanceIdentifier getRootPath() {
        return this.rootPath;
    }
}
