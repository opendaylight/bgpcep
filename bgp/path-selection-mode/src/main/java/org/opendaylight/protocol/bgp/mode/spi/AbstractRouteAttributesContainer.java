/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.spi;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.spi.policy.RouteAttributesContainerAdvertize;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

public abstract class AbstractRouteAttributesContainer implements RouteAttributesContainerAdvertize {
    private final PathArgument routeKey;
    private final PathArgument routeIdPA;
    private final PeerId toPeerId;

    public AbstractRouteAttributesContainer(@Nonnull final PathArgument key,
        @Nonnull final PeerId toPeerId, final PathArgument routeIdPA) {
        this.routeKey = key;
        this.toPeerId = toPeerId;
        this.routeIdPA = routeIdPA;
    }

    @Override
    public final PathArgument getRouteKey() {
        return this.routeKey;
    }

    @Override
    public final PeerId getToPeerId() {
        return this.toPeerId;
    }

    @Override
    public final PathArgument getRouteIdPA() {
        return this.routeIdPA;
    }
}
