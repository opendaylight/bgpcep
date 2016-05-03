/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.types.rev151018;

import org.junit.Assert;
import org.junit.Test;

public class RouteDistinguisherBuilderTest {

    private static final String RD = "127.0.0.1:123";

    @Test
    public void testRD() {
        final RouteDistinguisher rd = RouteDistinguisherBuilder.getDefaultInstance(RD);
        Assert.assertEquals(RD, rd.getString());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFailure() {
        RouteDistinguisherBuilder.getDefaultInstance("abc");
    }

}
