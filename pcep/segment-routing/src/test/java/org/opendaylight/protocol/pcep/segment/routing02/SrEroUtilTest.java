/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs5Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupTypeBuilder;

public class SrEroUtilTest {

    @Test(expected=UnsupportedOperationException.class)
    public void testPrivateConstructor() throws Throwable {
        final Constructor<SrEroUtil> c = SrEroUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testAddSRPathSetupTypeTlv() {
        final SrpBuilder srpBuilder = new SrpBuilder();
        final Srp srp = SrEroUtil.addSRPathSetupTypeTlv(srpBuilder.build());
        Assert.assertTrue(srp.getTlvs().getAugmentation(Tlvs5.class).getPathSetupType().isPst());
    }

    @Test
    public void testIsPst() {
        Assert.assertTrue(SrEroUtil.isPst(new Tlvs5Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build()));
        Assert.assertFalse(SrEroUtil.isPst(new Tlvs5Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(false).build()).build()));
        Assert.assertFalse(SrEroUtil.isPst(null));
        Assert.assertFalse(SrEroUtil.isPst(new Tlvs5Builder().build()));
    }
}
