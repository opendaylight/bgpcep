/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.route.target.impl.nlri;

import org.opendaylight.protocol.bgp.route.target.spi.nlri.RouteTargetConstrainParser;
import org.opendaylight.protocol.bgp.route.target.spi.nlri.RouteTargetConstrainSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.rev180618.route.target.constrain.RouteTargetConstrainChoice;

abstract class AbstractRouteTargetHandler<T extends RouteTargetConstrainChoice>
        implements RouteTargetConstrainParser<T>, RouteTargetConstrainSerializer<T> {
}
