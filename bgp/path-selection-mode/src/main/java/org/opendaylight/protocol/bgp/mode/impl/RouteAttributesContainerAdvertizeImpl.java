/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl;

import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.mode.spi.AbstractRouteAttributesContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

public final class RouteAttributesContainerAdvertizeImpl extends AbstractRouteAttributesContainer {
    public RouteAttributesContainerAdvertizeImpl(@Nonnull final PathArgument key, @Nonnull final PeerId toPeerId,
        @Nonnull final PathArgument routeIdPA) {
        super(key, toPeerId, routeIdPA);
    }
}
