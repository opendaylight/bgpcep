/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;

public class TlvUtilTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<TlvUtil> c = TlvUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
