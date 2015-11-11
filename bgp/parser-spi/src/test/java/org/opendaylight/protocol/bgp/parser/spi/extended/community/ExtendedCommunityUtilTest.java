/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Assert;
import org.junit.Test;

public class ExtendedCommunityUtilTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<ExtendedCommunityUtil> c = ExtendedCommunityUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testGetType() {
        assertEquals(1, ExtendedCommunityUtil.setTransitivity(1, true));
        assertEquals(65, ExtendedCommunityUtil.setTransitivity(1, false));
    }

    @Test
    public void testIsTransitiveType() {
        Assert.assertTrue(ExtendedCommunityUtil.isTransitive(2));
        Assert.assertFalse(ExtendedCommunityUtil.isTransitive(66));
    }

}
