/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.ImmutableMap;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathIdGrouping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public final class AdditionalPathUtil {
    private AdditionalPathUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Extract PathId from route change received.
     *
     * @param route Path Id Container
     * @return pathId  The path identifier value
     */
    public static long extractPathId(@Nonnull PathIdGrouping route) {
        final PathId pathContainer = route.getPathId();
        if (pathContainer == null || pathContainer.getValue() == null) {
            return PathIdUtil.NON_PATH_ID_VALUE;
        }
        return pathContainer.getValue();
    }

    public static NodeIdentifierWithPredicates createRouteKeyPathArgument(
            final QName routeQName,
            final QName routeKeyQname,
            final NodeIdentifierWithPredicates routeKey) {
        final ImmutableMap<QName, Object> keyValues = ImmutableMap.of(routeKeyQname,
                PathIdUtil.getObjectKey(routeKey, routeKeyQname));
        return new NodeIdentifierWithPredicates(routeQName, keyValues);
    }
}
