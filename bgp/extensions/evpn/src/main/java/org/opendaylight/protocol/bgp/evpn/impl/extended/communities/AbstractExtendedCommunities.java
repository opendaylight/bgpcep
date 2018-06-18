/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.extended.communities;

import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityUtil;

abstract class AbstractExtendedCommunities implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 6;

    @Override
    public final int getType(final boolean isTransitive) {
        return ExtendedCommunityUtil.setTransitivity(TYPE, isTransitive);
    }
}
