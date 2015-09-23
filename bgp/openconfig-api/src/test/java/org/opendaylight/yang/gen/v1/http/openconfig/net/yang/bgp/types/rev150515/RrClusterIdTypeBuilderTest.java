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

public class RrClusterIdTypeBuilderTest {

    @Test
    public void testIpv4() {
        final RrClusterIdType rrClusterType = RrClusterIdTypeBuilder.getDefaultInstance("127.0.0.1");
        Assert.assertEquals("127.0.0.1", rrClusterType.getIpv4Address().getValue());
    }

    @Test
    public void testUint32() {
        final RrClusterIdType rrClusterType = RrClusterIdTypeBuilder.getDefaultInstance("12345");
        Assert.assertEquals(12345L, rrClusterType.getUint32().longValue());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalid() {
        RrClusterIdTypeBuilder.getDefaultInstance("abcd");
    }

}
