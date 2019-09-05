/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetMedType.Enumeration;
import org.opendaylight.yangtools.yang.common.Uint32;

public class BgpSetMedTypeBuilderTest {

    @Test
    public void testString() {
        final BgpSetMedType medType = BgpSetMedTypeBuilder.getDefaultInstance("+1");
        assertEquals("+1", medType.getString());
    }

    @Test
    public void testUint32() {
        final BgpSetMedType medType = BgpSetMedTypeBuilder.getDefaultInstance("1");
        assertEquals(Uint32.ONE, medType.getUint32());
    }

    @Test
    public void testEnumLowerCase() {
        final BgpSetMedType medType = BgpSetMedTypeBuilder.getDefaultInstance("igp");
        assertEquals(Enumeration.IGP, medType.getEnumeration());
    }

    @Test
    public void testEnumFirstUpperCase() {
        final BgpSetMedType medType = BgpSetMedTypeBuilder.getDefaultInstance("Igp");
        assertEquals(Enumeration.IGP, medType.getEnumeration());
    }

    @Test
    public void testEnumUpperCase() {
        final BgpSetMedType medType = BgpSetMedTypeBuilder.getDefaultInstance("IGP");
        assertEquals(Enumeration.IGP, medType.getEnumeration());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidBigNumber() {
        BgpSetMedTypeBuilder.getDefaultInstance("4294967297");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidString() {
        BgpSetMedTypeBuilder.getDefaultInstance("abcd");
    }

}
