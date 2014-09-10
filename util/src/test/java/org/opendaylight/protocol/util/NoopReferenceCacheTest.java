/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.util;

import org.junit.Assert;
import org.junit.Test;

public class NoopReferenceCacheTest {

    private static final Object OBJ = new Object();

    @Test
    public void testSharedReference() {
        Assert.assertEquals(OBJ, NoopReferenceCache.getInstance().getSharedReference(OBJ));
    }
}
