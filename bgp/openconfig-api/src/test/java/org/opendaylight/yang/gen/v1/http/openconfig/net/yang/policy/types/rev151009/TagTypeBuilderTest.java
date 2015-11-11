/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009;

import org.junit.Assert;
import org.junit.Test;

public class TagTypeBuilderTest {

    @Test
    public void testUint32() {
        final TagType tagType = TagTypeBuilder.getDefaultInstance("12345");
        Assert.assertEquals(12345L, tagType.getUint32().longValue());
    }

    @Test
    public void testString() {
        final TagType tagType = TagTypeBuilder.getDefaultInstance("ab:cd");
        Assert.assertEquals("ab:cd", tagType.getHexString().getValue());
    }

}
