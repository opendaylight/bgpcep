/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;

public interface RouteTargetConstrainSerializer<T extends RouteTargetConstrainChoice> {
    /**
     * Serialize routeTarget.
     *
     * @param routeTarget routeTarget
     * @return Encode routeTarget to output buffer
     */
    @Nonnull
    ByteBuf serializeRouteTargetConstrain(@Nonnull T routeTarget);

    /**
     * returns class of RouteTargetChoice handled by serializer.
     *
     * @return RouteTargetChoice Class
     */
    @Nonnull
    Class<T> getClazz();

    /**
     * Returns NlriType handled by parser.
     *
     * @return NlriType
     */
    @Nullable
    Integer getType();
}
