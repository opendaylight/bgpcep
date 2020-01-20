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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

public class BandwidthTest {
    private Bandwidth b1;
    private Bandwidth b2;
    private Bandwidth b3;
    private Bandwidth b4;

    @Before
    public void setUp() {
        this.b1 = new Bandwidth(Unpooled.copyInt(1000).array());
        this.b2 = new Bandwidth(Unpooled.copyInt(2000).array());
        this.b3 = new Bandwidth(Unpooled.copyInt(2000).array());
        this.b4 = new Bandwidth(Unpooled.copyInt(100).array());
    }

    @Test
    public void testBitsBytes() {
        assertEquals(1000.0, Unpooled.wrappedBuffer(this.b1.getValue()).readInt(), 0.1);
    }

    @Test
    public void testEquals() {
        assertFalse(this.b1.equals(null));
        assertThat(this.b1, not(equalTo(new Object())));
        assertThat(this.b1, equalTo(this.b1));
        assertThat(this.b1, not(equalTo(this.b2)));
        assertEquals(this.b2, this.b3);
        assertNotEquals(this.b1, new Object());
    }

    @Test
    public void testHashCode() {
        final Set<Bandwidth> set = new HashSet<>();

        set.add(this.b1);
        assertEquals(1, set.size());

        set.add(this.b2);
        assertEquals(2, set.size());

        set.add(this.b3);
        assertEquals(2, set.size());

        set.add(this.b4);
        assertEquals(3, set.size());
    }
}
