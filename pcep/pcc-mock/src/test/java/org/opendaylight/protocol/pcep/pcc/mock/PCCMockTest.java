/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Assert;
import org.junit.Test;



public class PCCMockTest {

    @Test
    public void testSessionEstablishment() {
        try {
            org.opendaylight.protocol.pcep.testtool.Main.main(new String[]{"-a", "127.0.1.0:4189", "-ka", "10", "-d", "0", "--stateful", "--active"});
            Main.main(new String[] {"--local-address", "127.0.0.1", "--remote-address", "127.0.1.0", "--pcc", "2", "--lsp", "1", "--log-level", "DEBUG", "-ka", "10", "-d", "0",});
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMockPCCToManyPCE() {
        try {
            org.opendaylight.protocol.pcep.testtool.Main.main(new String[]{"-a", "127.0.4.0:4189", "-ka", "10", "-d", "0", "--stateful", "--active"});
            org.opendaylight.protocol.pcep.testtool.Main.main(new String[]{"-a", "127.0.2.0:4189", "-ka", "10", "-d", "0", "--stateful", "--active"});
            org.opendaylight.protocol.pcep.testtool.Main.main(new String[]{"-a", "127.0.3.0:4189", "-ka", "10", "-d", "0", "--stateful", "--active"});
            Main.main(new String[] {"--local-address", "127.0.0.1", "--remote-address", "127.0.4.0,127.0.2.0,127.0.3.0", "--pcc", "3"});
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<MsgBuilderUtil> c = MsgBuilderUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
