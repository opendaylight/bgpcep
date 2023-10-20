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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yangtools.yang.common.Uint32;

class ASNumberTest {
    private static final AsNumber ASN1 = new AsNumber(Uint32.valueOf(4294967295L));
    private static final AsNumber ASN3 = new AsNumber(Uint32.valueOf(200));
    private static final AsNumber ASN4 = new AsNumber(Uint32.valueOf(429496335L));

    @Test
    void testHashCode() {
        final var set = new HashSet<AsNumber>();

        set.add(ASN1);
        assertEquals(1, set.size());

        set.add(ASN3);
        assertEquals(2, set.size());
    }

    @Test
    void testGetters() {
        assertEquals(4294967295L, ASN1.getValue().longValue());
    }

    @Test
    void testEquals() {
        assertThat(ASN1, not(equalTo(ASN3)));
        assertThat(ASN1, not(equalTo(ASN4)));
        assertThat(ASN1, not(equalTo(new Object())));
        assertNotEquals(ASN1, new Object());
    }

    @Test
    void testToString() {
        assertEquals("AsNumber{value=4294967295}", ASN1.toString());
        assertEquals("AsNumber{value=200}", ASN3.toString());
    }
}
