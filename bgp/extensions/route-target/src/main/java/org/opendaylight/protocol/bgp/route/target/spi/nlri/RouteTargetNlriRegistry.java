/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.spi.nlri;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.RouteTargetChoice;

public interface RouteTargetNlriRegistry {
    /**
     * Decode input buffer to BGP Route Target.
     *
     * @param type   Nlri Type
     * @param buffer encoded RouteTargetChoice body in Bytebuf
     * @return RouteTargetChoice
     */
    @Nullable
    RouteTargetChoice parseRouteTarget(@Nullable Integer type, @Nonnull ByteBuf buffer);

    /**
     * Encode input BGP RouteTarget to output buffer.
     *
     * @param RouteTarget RouteTargetChoice
     * @return encoded RouteTargetChoice body in Bytebuf
     */
    @Nonnull
    ByteBuf serializeRouteTarget(@Nonnull RouteTargetChoice RouteTarget);
}
