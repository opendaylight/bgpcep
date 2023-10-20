/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.netty.buffer.Unpooled;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

class BandwidthTest {
    private static final Bandwidth B1 = new Bandwidth(Unpooled.copyInt(1000).array());
    private static final Bandwidth B2 = new Bandwidth(Unpooled.copyInt(2000).array());
    private static final Bandwidth B3 = new Bandwidth(Unpooled.copyInt(2000).array());
    private static final Bandwidth B4 = new Bandwidth(Unpooled.copyInt(100).array());

    @Test
    void testBitsBytes() {
        assertEquals(1000.0, Unpooled.wrappedBuffer(B1.getValue()).readInt(), 0.1);
    }

    @Test
    void testEquals() {
        assertFalse(B1.equals(null));
        assertThat(B1, not(equalTo(new Object())));
        assertThat(B1, equalTo(B1));
        assertThat(B1, not(equalTo(B2)));
        assertEquals(B2, B3);
        assertNotEquals(B1, new Object());
    }

    @Test
    void testHashCode() {
        final var set = new HashSet<Bandwidth>();

        set.add(B1);
        assertEquals(1, set.size());

        set.add(B2);
        assertEquals(2, set.size());

        set.add(B3);
        assertEquals(2, set.size());

        set.add(B4);
        assertEquals(3, set.size());
    }
}
