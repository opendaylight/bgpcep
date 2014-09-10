/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder;



public class PCCMockTest {

    @Test
    public void testSessionEstablishment() {
        try {
            org.opendaylight.protocol.pcep.testtool.Main.main(new String[]{"-a", "127.0.1.0:4189", "-ka", "10", "-d", "0", "--stateful", "--active"});
            Main.main(new String[] {"--address", "127.0.1.0", "--pcc", "1", "--lsp", "1"});
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testSrpObjectParser() throws PCEPDeserializerException {
        final byte[] bytes = {
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
        };
        final PCEPExtensionProviderContext ctx = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance();
        final PCCSrpObjectParser parser = new PCCSrpObjectParser(ctx.getTlvHandlerRegistry(), ctx.getVendorInformationTlvRegistry());
        final Srp srp = new SrpBuilder().setIgnore(true).setProcessingRule(true).setOperationId(new SrpIdNumber(0L)).setTlvs(new TlvsBuilder().build()).build();
        Assert.assertEquals(srp, parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.wrappedBuffer(bytes)));
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
