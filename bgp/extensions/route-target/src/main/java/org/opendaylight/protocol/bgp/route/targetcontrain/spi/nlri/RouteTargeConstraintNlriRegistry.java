/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.spi.nlri;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.route.target.constrain.RouteTargetConstrainChoice;

@NonNullByDefault
public interface RouteTargeConstraintNlriRegistry {
    /**
     * Decode input buffer to BGP Route Target.
     *
     * @param type   Nlri Type
     * @param buffer encoded RouteTargetChoice body in Bytebuf
     * @return RouteTargetChoice
     */
    @Nullable RouteTargetConstrainChoice parseRouteTargetConstrain(@Nullable Integer type, ByteBuf buffer);

    /**
     * Encode input BGP routeTarget to output buffer.
     *
     * @param routeTarget RouteTargetChoice
     * @return encoded RouteTargetChoice body in Bytebuf
     */
    ByteBuf serializeRouteTargetConstrain(RouteTargetConstrainChoice routeTarget);
}
