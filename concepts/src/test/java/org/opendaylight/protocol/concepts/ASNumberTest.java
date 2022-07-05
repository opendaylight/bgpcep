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
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ASNumberTest {
    private AsNumber asn1;
    private AsNumber asn3;
    private AsNumber asn4;

    @Before
    public void setUp() {
        asn1 = new AsNumber(Uint32.valueOf(4294967295L));
        asn3 = new AsNumber(Uint32.valueOf(200));
        asn4 = new AsNumber(Uint32.valueOf(429496335L));
    }

    @Test
    public void testHashCode() {
        final Set<AsNumber> set = new HashSet<>();

        set.add(asn1);
        assertEquals(1, set.size());

        set.add(asn3);
        assertEquals(2, set.size());
    }

    @Test
    public void testGetters() {
        assertEquals(4294967295L, asn1.getValue().longValue());
    }

    @Test
    public void testEquals() {
        assertThat(asn1, not(equalTo(asn3)));
        assertThat(asn1, not(equalTo(asn4)));
        assertThat(asn1, not(equalTo(new Object())));
        assertNotEquals(asn1, new Object());
    }

    @Test
    public void testToString() {
        assertEquals("AsNumber{value=4294967295}", asn1.toString());
        assertEquals("AsNumber{value=200}", asn3.toString());
    }
}
