/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi.policy;

import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;

public interface RTCCache {
    /**
     * Cache of RTC routes advertized per clients peers.
     *
     * @return rtc cache
     */
    @Nonnull
    List<Route> getClientRouteTargetContrainCache();
}
