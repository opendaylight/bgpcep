/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515;

import org.junit.Assert;
import org.junit.Test;

public class BgpStdCommunityTypeBuilderTest {

    @Test
    public void testString() {
        final BgpStdCommunityType commType = BgpStdCommunityTypeBuilder.getDefaultInstance("72:123");
        Assert.assertEquals("72:123", commType.getString());
    }

    @Test
    public void testUint32() {
        final BgpStdCommunityType commType = BgpStdCommunityTypeBuilder.getDefaultInstance("123");
        Assert.assertEquals(123L, commType.getUint32().longValue());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalid() {
        BgpStdCommunityTypeBuilder.getDefaultInstance("-1");
    }

}
