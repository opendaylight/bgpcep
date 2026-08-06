/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;

class AbstractTwoOctetAsExtendedCommunityTest {
    @Test
    void testGetType() {
        final AbstractTwoOctetAsExtendedCommunity community = new AbstractTwoOctetAsExtendedCommunity() {
            @Override
            public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity,
                    final ByteBuf byteAggregator) {
                // No-op
            }

            @Override
            public int getSubType() {
                return 0;
            }

            @Override
            public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer)  {
                return null;
            }
        };
        assertEquals(0, community.getType(true));
        assertEquals(64, community.getType(false));
    }
}
