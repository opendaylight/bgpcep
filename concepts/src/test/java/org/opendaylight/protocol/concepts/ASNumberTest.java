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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;

public class ASNumberTest {
    private AsNumber asn1, asn3, asn4;

    @Before
    public void setUp() {
        this.asn1 = new AsNumber(4294967295L);
        this.asn3 = new AsNumber((long) 200);
        this.asn4 = new AsNumber(429496335L);
    }

    @Test
    public void testHashCode() {
        final Set<AsNumber> set = new HashSet<>();

        set.add(this.asn1);
        assertEquals(1, set.size());

        set.add(this.asn3);
        assertEquals(2, set.size());
    }

    @Test
    public void testGetters() {
        assertEquals(4294967295L, this.asn1.getValue().longValue());
    }

    @Test
    public void testEquals() {
        assertThat(this.asn1, not(equalTo(this.asn3)));
        assertThat(this.asn1, not(equalTo(this.asn4)));
        assertThat(this.asn1, not(equalTo(new Object())));
        assertFalse(this.asn1.equals(new Object()));
    }

    @Test
    public void testToString() {
        assertEquals("AsNumber [_value=4294967295]", this.asn1.toString());
        assertEquals("AsNumber [_value=200]", this.asn3.toString());
    }
}
